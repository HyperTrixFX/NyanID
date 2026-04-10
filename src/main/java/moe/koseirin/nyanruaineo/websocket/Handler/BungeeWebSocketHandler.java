package moe.koseirin.nyanruaineo.websocket.Handler;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.Packet.Client.BindAccountPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.CheckBindPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.HeartbeatResponsePacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.UpdateOnlinePacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.CheckBindResponsePacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.HeartbeatPacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.UpdateOnlineResponsePacket;
import moe.koseirin.nyanruaineo.network.utils.KeyManager;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;
import moe.koseirin.nyanruaineo.network.utils.PacketRegistry;
import moe.koseirin.nyanruaineo.network.utils.SM4Util;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.ServerListRepository;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class BungeeWebSocketHandler extends BinaryWebSocketHandler {

    private final ServerListRepository serverListRepository;
    private final RedisService redisService;
    private final AccountsRepository accountsRepository;
    private final KeyManager KeyManager;
    private final PacketRegistry packetRegistry;
    private final Map<String, WebSocketSession> authenticatedSessions = new ConcurrentHashMap<>();

    public BungeeWebSocketHandler(ServerListRepository serverListRepository, RedisService redisService, AccountsRepository accountsRepository, KeyManager KeyManager, PacketRegistry packetRegistry) {
        this.serverListRepository = serverListRepository;
        this.redisService = redisService;
        this.accountsRepository = accountsRepository;
        this.KeyManager = KeyManager;
        this.packetRegistry = packetRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        String sid = (String) attributes.get("sid");
        String key = (String) attributes.get("key");

        if (sid == null || key == null) {
            log.error("握手阶段未提供认证信息喵（X-SID / X-KEY），关闭连接喵，SessionId: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("检测到BungeeCord连接请求喵，Sid: [{}], SessionId: {}", sid, session.getId());

        String token = serverListRepository.findTokenByServerUid(sid);
        if (token != null && Objects.equals(key, token)) {
            authenticatedSessions.put(session.getId(), session);
            log.info("BungeeCord连接认证成功喵，SessionId: {}", session.getId());
        } else {
            log.error("认证失败，无效的 sid 或 key，SessionId喵: {}", session.getId());
            session.close(new CloseStatus(4500, "10001"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, @NonNull BinaryMessage message) throws Exception {
        if (!authenticatedSessions.containsKey(session.getId())) {
            log.warn("未认证的会话尝试发送消息，关闭连接喵，SessionId: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] encryptedData = new byte[payload.remaining()];
        payload.get(encryptedData);

        // check empty data
        if (encryptedData.length == 0) {
            log.error("收到空消息，关闭连接喵，SessionId: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        byte[] key = KeyManager.getKey(session.getId());
        byte[] plainData;
        try {
            plainData = SM4Util.decrypt(encryptedData, key);
        } catch (IllegalArgumentException e) {
            log.error("解密参数错误喵: {}, SessionId: {}", e.getMessage(), session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        } catch (javax.crypto.IllegalBlockSizeException e) {
            log.error("解密失败，密文长度不正确（可能不是16的倍数）喵: {}, SessionId: {}", e.getMessage(), session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        } catch (javax.crypto.BadPaddingException e) {
            log.error("解密失败，填充错误（可能密钥不正确）喵: {}, SessionId: {}", e.getMessage(), session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        } catch (Exception e) {
            log.error("解密消息失败，关闭连接喵，SessionId: {}", session.getId(), e);
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        ByteBuf buf = Unpooled.wrappedBuffer(plainData);
        try {
            int packetId = PacketCodecUtil.readVarInt(buf);
            Packet packet = packetRegistry.createPacket(packetId);
            packet.decode(buf);
            handlePacket(session, packet);
        } catch (Exception e) {
            log.error("处理数据包异常喵，SessionId: {}", session.getId(), e);
            session.close(CloseStatus.PROTOCOL_ERROR);
        } finally {
            buf.release();
        }
    }

    private void handlePacket(WebSocketSession session, Packet packet) throws IOException {
        if (packet instanceof HeartbeatPacket) {
            sendPacket(session, new HeartbeatResponsePacket());
        }
        else if (packet instanceof UpdateOnlinePacket p) {
            try {
                redisService.setValueWithExpiration("Online-" + p.getServername(), p.getOnline(), 30, TimeUnit.SECONDS);
                sendPacket(session, new UpdateOnlineResponsePacket(true));
            } catch (Exception e) {
                log.error("更新在线人数失败喵", e);
                sendPacket(session, new UpdateOnlineResponsePacket(false));
            }
        }
        else if (packet instanceof BindAccountPacket p) {
            try {
                redisService.setValueWithExpiration(p.getCode(), p.getUuid(), 180, TimeUnit.SECONDS);
                sendPacket(session, new UpdateOnlineResponsePacket(true));
            } catch (Exception e) {
                log.error("绑定账号失败喵", e);
                sendPacket(session, new UpdateOnlineResponsePacket(false));
            }
        }
        else if (packet instanceof CheckBindPacket p) {
            try {
                Accounts accounts = accountsRepository.GetUser(p.getUuid());
                if (accounts == null) {
                    sendPacket(session, new CheckBindResponsePacket(false, null, null, null));
                } else {
                    sendPacket(session, new CheckBindResponsePacket(true, accounts.getBind(), accounts.getUid(), accounts.getUsername()));
                }
            } catch (Exception e) {
                log.error("检查绑定失败喵", e);
                session.close(CloseStatus.PROTOCOL_ERROR);
            }
        }
        else {
            log.error("未知的包类型喵: {}", packet.getClass().getSimpleName());
            session.close(new CloseStatus(4500, "10001"));
        }
    }

    private void sendPacket(WebSocketSession session, Packet packet) throws IOException {
        ByteBuf buf = Unpooled.buffer();
        try {
            PacketCodecUtil.writeVarInt(buf, packet.packetId());
            packet.encode(buf);

            byte[] plainData = new byte[buf.readableBytes()];
            buf.readBytes(plainData);

            byte[] key = KeyManager.getKey(session.getId());
            byte[] encryptedData = SM4Util.encrypt(plainData, key);

            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(encryptedData)));
        } catch (Exception e) {
            log.error("发送数据包失败喵", e);
            throw new IOException("发送失败喵", e);
        } finally {
            buf.release();
        }
    }

    public void broadcastPacket(Packet packet) {
        for (WebSocketSession session : authenticatedSessions.values()) {
            try {
                if (session.isOpen()) {
                    sendPacket(session, packet);
                }
            } catch (IOException e) {
                log.error("广播包失败喵，SessionId: {}", session.getId(), e);
            }
        }
    }

    public void broadcastHeartbeat() {
        HeartbeatResponsePacket packet = new HeartbeatResponsePacket();
        for (WebSocketSession session : authenticatedSessions.values()) {
            try {
                if (session.isOpen()) {
                    sendPacket(session, packet);
                }
            } catch (IOException e) {
                log.error("广播心跳失败喵，SessionId: {}", session.getId(), e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        authenticatedSessions.remove(session.getId());
        log.warn("Bungee连接断开喵，SessionId: {}, 状态: {}", session.getId(), status);
    }
}
