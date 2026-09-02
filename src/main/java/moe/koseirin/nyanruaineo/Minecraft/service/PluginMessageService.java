package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.ProtocolConstants;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 代理端的插件消息系统一侧
 * （代理端自身的 {@code REGISTER}、客户端已注册频道和品牌的回放
 * 以及 {@code Forward} 所使用的每个服务器的数据包队列）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginMessageService {

    private final MinecraftProxy proxy;
    private final ProxyProperties properties;
    private final BackendServerManager backendServerManager;
    private final PlayerMessageService playerMessageService;
    // These three depend back on MinecraftProxy; @Lazy breaks the Spring constructor cycle.
    @Lazy
    private final PlayerKickService playerKickService;
    @Lazy
    private final PlayerTransferService playerTransferService;
    @Lazy
    private final PlayerQueryService playerQueryService;

    /**
     * Plugin messages queued for backends with no online players, keyed by {@code host:port}
     * (BungeeCord's per-{@code ServerInfo} packet queue, drained on the next connect).
     */
    private final Map<String, Queue<PluginMessage>> packetQueue = new ConcurrentHashMap<>();

    /**
     * 后端会通过每个在线玩家的连接重复下发同一条 {@code Connect}/{@code ConnectOther}；
     * 用 (目标玩家, 服务器) 加一个短时窗口去重，保证每个逻辑请求只被处理一次。
     * 访问都在 {@code connectDedupLock} 下，普通 {@link HashMap} 即可，无需额外并发结构。
     */
    private final Map<String, Long> recentConnectRequests = new HashMap<>();
    private final Object connectDedupLock = new Object();
    private static final long CONNECT_DEDUP_WINDOW_MS = 1_000L;


    /**
     * Builds the {@code REGISTER} packet that tells a backend (or the client) which channels the
     * proxy itself owns, mirroring BungeeCord's {@code BungeeCord.registerChannels(int)}.
     */
    public PluginMessage registerChannels(int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
            return new PluginMessage("minecraft:register",
                    PluginMessage.modernise(PluginMessage.BUNGEE_CHANNEL_LEGACY).getBytes(StandardCharsets.UTF_8));
        }
        return new PluginMessage("REGISTER",
                PluginMessage.BUNGEE_CHANNEL_LEGACY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Builds the proxy's own brand plugin message, mirroring BungeeCord's first-join brand: the
     * payload is a VarInt-prefixed string ({@code proxyName (version)}).
     */
    public PluginMessage buildProxyBrand(int protocolVersion) {
        String brand = properties.getProxyName() + " (" + proxyVersion() + ")";
        ByteBuf buf = Unpooled.buffer();
        try {
            DefinedPacket.writeString(brand, buf);
            return new PluginMessage(protocolVersion >= ProtocolConstants.MINECRAFT_1_13
                    ? "minecraft:brand" : "MC|Brand", DefinedPacket.toArray(buf));
        } finally {
            buf.release();
        }
    }

    /** The proxy version shown in the brand message (BungeeCord shows its build version). */
    private static String proxyVersion() {
        return "1.0";
    }

    /**
     * Handles one message on the {@code BungeeCord} channel coming FROM a backend, mirroring
     * BungeeCord's {@code DownstreamBridge} switch. Replies are written back along the same
     * channel; {@code Forward}/{@code ForwardToPlayer} re-route the payload to another backend.
     */
    public void handleBungeeCordChannel(UserConnection user, ServerConnection server, byte[] data) {
        ByteArrayOutputStream replyBuffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(replyBuffer);
        try {
            DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(data));
            String subChannel = in.readUTF();

            switch (subChannel) {
                case "ForwardToPlayer" -> {
                    UserConnection target = playerQueryService.getUserConnection(in.readUTF());
                    if (target != null) {
                        String channel = in.readUTF();
                        short len = in.readShort();
                        byte[] payload = new byte[len];
                        in.readFully(payload);

                        ByteArrayOutputStream forwardBuffer = new ByteArrayOutputStream();
                        DataOutputStream forward = new DataOutputStream(forwardBuffer);
                        forward.writeUTF(channel);
                        forward.writeShort(payload.length);
                        forward.write(payload);
                        byte[] forwarded = forwardBuffer.toByteArray();

                        ServerConnection targetServer = target.getServer();
                        if (targetServer != null && targetServer.getChannel().isActive()) {
                            targetServer.sendData(PluginMessage.BUNGEE_CHANNEL_LEGACY, forwarded);
                        }
                    }
                    // Null out stream: we don't want to reply to ourselves.
                    out = null;
                    replyBuffer = null;
                }
                case "Forward" -> {
                    String target = in.readUTF();
                    String channel = in.readUTF();
                    short len = in.readShort();
                    byte[] payload = new byte[len];
                    in.readFully(payload);

                    ByteArrayOutputStream forwardBuffer = new ByteArrayOutputStream();
                    DataOutputStream forward = new DataOutputStream(forwardBuffer);
                    forward.writeUTF(channel);
                    forward.writeShort(payload.length);
                    forward.write(payload);
                    byte[] forwarded = forwardBuffer.toByteArray();

                    // Null out stream: we don't want to reply to ourselves.
                    out = null;
                    replyBuffer = null;

                    switch (target) {
                        case "ALL" -> {
                            for (BackendServer serverInfo : backendServerManager.listServers()) {
                                if (!sameAddress(serverInfo, server)) {
                                    sendDataToServer(serverInfo, forwarded, true);
                                }
                            }
                        }
                        case "ONLINE" -> {
                            for (BackendServer serverInfo : backendServerManager.listServers()) {
                                if (!sameAddress(serverInfo, server)) {
                                    sendDataToServer(serverInfo, forwarded, false);
                                }
                            }
                        }
                        default -> {
                            BackendServer serverInfo = backendServerManager.findByName(target);
                            if (serverInfo != null) {
                                sendDataToServer(serverInfo, forwarded, true);
                            }
                        }
                    }
                }
                case "Connect", "ConnectOther" -> {
                    String targetName = in.readUTF();
                    String serverName = in.readUTF();
                    if (!claimConnect(targetName, serverName)) {
                        break;
                    }
                    UserConnection target = playerQueryService.getUserConnection(targetName);
                    BackendServer serverInfo = backendServerManager.findByName(serverName);
                    if (target != null && serverInfo != null) {
                        ServerConnection current = target.getServer();
                        if (current != null && !current.isClosed()
                                && serverInfo.getHost() != null && serverInfo.getHost().equalsIgnoreCase(current.getHost())
                                && serverInfo.getPort() == current.getPort()) {
                            proxy.getPlayerMessageService().sendMessage(target, "§eYou are already connected to " + serverInfo.getName() + "!");
                        }else {
                            playerTransferService.transferIfOnline(target, serverInfo,
                                    error -> {});

                        }
                    }else {
                        if (target != null) {
                            proxy.getPlayerMessageService().sendMessage(target, "§e不存在该子服务器节点!");
                        }
                    }
                }
                case "GetPlayerServer" -> {
                    String name = in.readUTF();
                    UserConnection target = playerQueryService.getUserConnection(name);
                    out.writeUTF("GetPlayerServer");
                    out.writeUTF(name);
                    out.writeUTF(target != null && target.getServer() != null
                            ? resolveServerName(target.getServer()) : "");
                }
                case "IP" -> {
                    out.writeUTF("IP");
                    InetSocketAddress address = socketAddress(user);
                    if (address != null) {
                        out.writeUTF(address.getAddress().getHostAddress());
                        out.writeInt(address.getPort());
                    } else {
                        out.writeUTF("unknown");
                        out.writeInt(0);
                    }
                }
                case "IPOther" -> {
                    UserConnection target = playerQueryService.getUserConnection(in.readUTF());
                    if (target != null) {
                        out.writeUTF("IPOther");
                        out.writeUTF(target.getUsername());
                        InetSocketAddress address = socketAddress(target);
                        if (address != null) {
                            out.writeUTF(address.getAddress().getHostAddress());
                            out.writeInt(address.getPort());
                        } else {
                            out.writeUTF("unknown");
                            out.writeInt(0);
                        }
                    }
                }
                case "PlayerCount" -> {
                    String target = in.readUTF();
                    out.writeUTF("PlayerCount");
                    if (target.equals("ALL")) {
                        out.writeUTF("ALL");
                        out.writeInt(proxy.getOnlineCount());
                    } else {
                        BackendServer serverInfo = backendServerManager.findByName(target);
                        if (serverInfo != null) {
                            out.writeUTF(serverInfo.getName());
                            out.writeInt(playerQueryService.countPlayersOn(serverInfo.getHost(), serverInfo.getPort()));
                        }
                    }
                }
                case "PlayerList" -> {
                    String target = in.readUTF();
                    out.writeUTF("PlayerList");
                    if (target.equals("ALL")) {
                        out.writeUTF("ALL");
                        out.writeUTF(csv(proxy.getOnlineUsers().stream()
                                .map(UserConnection::getUsername).toList()));
                    } else {
                        BackendServer serverInfo = backendServerManager.findByName(target);
                        if (serverInfo != null) {
                            out.writeUTF(serverInfo.getName());
                            out.writeUTF(csv(playersOn(serverInfo.getHost(), serverInfo.getPort())));
                        }
                    }
                }
                case "GetServers" -> {
                    out.writeUTF("GetServers");
                    out.writeUTF(csv(backendServerManager.serverNames()));
                }
                case "Message" -> {
                    String target = in.readUTF();
                    String message = in.readUTF();
                    if (target.equals("ALL")) {
                        playerMessageService.broadcast(proxy.getOnlineUsers(), message);
                    } else {
                        UserConnection targetPlayer = playerQueryService.getUserConnection(target);
                        if (targetPlayer != null) {
                            playerMessageService.sendMessage(targetPlayer, message);
                        }
                    }
                }
                case "MessageRaw" -> {
                    String target = in.readUTF();
                    JSONObject component = parseComponent(in.readUTF());
                    if (component == null) {
                        break;
                    }
                    if (target.equals("ALL")) {
                        playerMessageService.broadcastRaw(proxy.getOnlineUsers(), component);
                    } else {
                        UserConnection targetPlayer = playerQueryService.getUserConnection(target);
                        if (targetPlayer != null) {
                            playerMessageService.sendRaw(targetPlayer, component);
                        }
                    }
                }
                case "GetServer" -> {
                    out.writeUTF("GetServer");
                    out.writeUTF(resolveServerName(server));
                }
                case "UUID" -> {
                    out.writeUTF("UUID");
                    out.writeUTF(user.getUuid().toString());
                }
                case "UUIDOther" -> {
                    UserConnection target = playerQueryService.getUserConnection(in.readUTF());
                    if (target != null) {
                        out.writeUTF("UUIDOther");
                        out.writeUTF(target.getUsername());
                        out.writeUTF(target.getUuid().toString());
                    }
                }
                case "ServerIP" -> {
                    BackendServer serverInfo = backendServerManager.findByName(in.readUTF());
                    if (serverInfo != null) {
                        try {
                            // BungeeCord skips unresolvable addresses; a failed lookup here simply
                            // produces no reply (typical hosts are IP literals or localhost).
                            String address = InetAddress.getByName(serverInfo.getHost()).getHostAddress();
                            out.writeUTF("ServerIP");
                            out.writeUTF(serverInfo.getName());
                            out.writeUTF(address);
                            out.writeShort(serverInfo.getPort());
                        } catch (UnknownHostException ignored) {
                            // No reply, like BungeeCord's isUnresolved() check.
                        }
                    }
                }
                case "KickPlayer" -> {
                    UserConnection target = playerQueryService.getUserConnection(in.readUTF());
                    if (target != null) {
                        String kickReason = in.readUTF();
                        playerKickService.kick(target, kickReason,null);
                    }
                }
                case "KickPlayerRaw" -> {
                    UserConnection target = playerQueryService.getUserConnection(in.readUTF());
                    if (target != null) {
                        JSONObject component = parseComponent(in.readUTF());
                        String kickReason = component == null ? "" : flattenText(component);
                        playerKickService.kick(target, kickReason,null);
                    }
                }
                default -> log.debug("Unknown BungeeCord channel sub-command '{}' from {}",
                        subChannel, server.getHost());
            }

            // Reply back along the BungeeCord channel (unless the sub-command nulled the stream).
            if (out != null) {
                byte[] b = replyBuffer.toByteArray();
                if (b.length != 0) {
                    server.sendData(PluginMessage.BUNGEE_CHANNEL_LEGACY, b);
                }
            }
        } catch (IOException e) {
            log.warn("Malformed BungeeCord channel message from {}: {}", server.getHost(), e.getMessage());
        }
    }

    /**
     * 去重判断：返回 {@code true} 表示这条 {@code Connect}/{@code ConnectOther} 应该被处理，
     * {@code false} 表示它是窗口期内重复下发的一份拷贝。窗口很短（约 1 秒），足够吞掉后端
     * 同时通过多条玩家连接下发的重复包，又不影响正常的后续传送请求。
     */
    private boolean claimConnect(String targetName, String serverName) {
        String key = targetName + '\0' + serverName;
        long now = System.currentTimeMillis();
        synchronized (connectDedupLock) {
            // 机会式清理，防止 map 无限增长。
            if (recentConnectRequests.size() > 256) {
                recentConnectRequests.values().removeIf(ts -> now - ts >= CONNECT_DEDUP_WINDOW_MS);
            }
            Long prev = recentConnectRequests.get(key);
            if (prev != null && now - prev < CONNECT_DEDUP_WINDOW_MS) {
                return false;
            }
            recentConnectRequests.put(key, now);
            return true;
        }
    }

    /**
     * Sends a {@code BungeeCord} channel payload to the players of the given backend — once per
     * server, via any online player's connection. When the server has no online players the
     * payload is queued (BungeeCord's {@code ServerInfo.sendData}); {@code queue=false} drops it
     * instead.
     */
    private void sendDataToServer(BackendServer target, byte[] payload, boolean queue) {
        ServerConnection connection = firstPlayerConnection(target);
        if (connection != null) {
            connection.sendData(PluginMessage.BUNGEE_CHANNEL_LEGACY, payload);
            return;
        }
        if (queue) {
            packetQueue.computeIfAbsent(key(target), k -> new ArrayDeque<>())
                    .add(new PluginMessage(PluginMessage.BUNGEE_CHANNEL_LEGACY, payload));
        }
    }

    /**
     * Delivers every queued plugin message for a backend to its freshly connected channel
     * (BungeeCord drains the {@code ServerInfo} packet queue in {@code handleLogin}).
     */
    public void drainQueue(BackendServer target, ServerConnection connection) {
        Queue<PluginMessage> queued = packetQueue.remove(key(target));
        if (queued == null) {
            return;
        }
        PluginMessage message;
        while ((message = queued.poll()) != null) {
            connection.sendPacket(message);
        }
    }

    private ServerConnection firstPlayerConnection(BackendServer target) {
        for (UserConnection user : proxy.getOnlineUsers()) {
            ServerConnection server = user.getServer();
            if (server != null && sameAddress(target, server)) {
                return server;
            }
        }
        return null;
    }

    private List<String> playersOn(String host, int port) {
        List<String> names = new ArrayList<>();
        for (UserConnection user : proxy.getOnlineUsers()) {
            ServerConnection server = user.getServer();
            if (server != null && host != null && host.equalsIgnoreCase(server.getHost())
                    && port == server.getPort()) {
                names.add(user.getUsername());
            }
        }
        return names;
    }

    /** Resolves a backend connection to its configured server name, falling back to host:port. */
    private String resolveServerName(ServerConnection server) {
        if (server == null) {
            return "";
        }
        for (BackendServer backend : backendServerManager.listServers()) {
            if (backend.getHost() != null && backend.getHost().equalsIgnoreCase(server.getHost())
                    && backend.getPort() == server.getPort()) {
                return backend.getName();
            }
        }
        return server.getHost() + ":" + server.getPort();
    }

    private static boolean sameAddress(BackendServer backend, ServerConnection server) {
        return backend.getHost() != null && backend.getHost().equalsIgnoreCase(server.getHost())
                && backend.getPort() == server.getPort();
    }

    private static String key(BackendServer server) {
        return server.getHost().toLowerCase(java.util.Locale.ROOT) + ":" + server.getPort();
    }

    private static InetSocketAddress socketAddress(UserConnection user) {
        if (user.getChannel() != null && user.getChannel().remoteAddress() instanceof InetSocketAddress address) {
            return address;
        }
        return null;
    }

    private static String csv(List<String> values) {
        return String.join(",", values);
    }

    /** Parses a chat component JSON string, tolerating {@code null} input. */
    private static JSONObject parseComponent(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            Object parsed = JSON.parse(json);
            return parsed instanceof JSONObject object ? object
                    : (parsed instanceof JSONArray array ? new JSONObject().fluentPut("extra", array)
                    : new JSONObject().fluentPut("text", String.valueOf(parsed)));
        } catch (Exception e) {
            log.warn("MessageRaw carried invalid component JSON: {}", e.getMessage());
            return null;
        }
    }

    /** Flattens a chat component tree into its plain text (used by KickPlayerRaw). */
    private static String flattenText(JSONObject component) {
        StringBuilder text = new StringBuilder();
        appendText(component, text);
        return text.toString();
    }

    private static void appendText(JSONObject component, StringBuilder text) {
        if (component == null) {
            return;
        }
        String value = component.getString("text");
        if (value != null) {
            text.append(value);
        }
        JSONArray extra = component.getJSONArray("extra");
        if (extra != null) {
            for (Object child : extra) {
                if (child instanceof JSONObject object) {
                    appendText(object, text);
                }
            }
        }
    }
}
