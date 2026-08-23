package moe.koseirin.nyanruaineo.network.Minecraft.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerDisconnectEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerJoinEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerLoginEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.ProxyPingEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.HandlerBoss;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PacketDecoder;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PacketEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PipelineUtils;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.EncryptionRequest;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.EncryptionResponse;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.ForgeHandshake;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.Handshake;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.LoginRequest;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.LoginSuccess;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PingPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.StatusRequest;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.StatusResponse;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Handles the handshake, status and login phases for an incoming client connection, mirroring
 * BungeeCord's {@code InitialHandler}. Once login completes it hands the channel over to an
 * {@link UpstreamBridge} and delegates backend connection setup to a {@link ServerConnector}.
 */
@Slf4j
public class InitialHandler extends ChannelInboundHandlerAdapter {

    private final MinecraftProxy proxy;
    private final Channel channel;

    private Protocol protocol = Protocol.HANDSHAKE;
    private int protocolVersion = -1;

    private String username;
    private UUID uuid;
    private UUID loginUuid;
    private String requestedServer;
    /** Extra data appended to the handshake host after the first NUL byte (e.g. the FML token). */
    private String extraDataInHandshake = "";
    private List<LoginSuccess.Property> properties = Collections.emptyList();
    private byte[] verifyToken;
    private KeyPair keyPair;
    private ServerConnection server;
    private UserConnection user;
    private boolean loginInProgress;

    public InitialHandler(MinecraftProxy proxy, Channel channel) {
        this.proxy = proxy;
        this.channel = channel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Handshake handshake) {
            handleHandshake(handshake);
        } else if (msg instanceof StatusRequest) {
            handleStatusRequest();
        } else if (msg instanceof PingPacket ping) {
            handlePing(ping);
        } else if (msg instanceof ForgeHandshake forgeHandshake) {
            handleForgeHandshake(forgeHandshake);
        } else if (msg instanceof LoginRequest loginRequest) {
            handleLoginRequest(loginRequest);
        } else if (msg instanceof EncryptionResponse response) {
            handleEncryptionResponse(response);
        } else if (msg instanceof ByteBuf buf) {
            // Unknown packet in a pre-play state: relay it to the backend if one is connected
            // (login plugin messages, cookies, etc.), otherwise drop it.
            ServerConnection backend = user == null ? null : user.getServer();
            if (backend != null && backend.getChannel().isActive()) {
                backend.getChannel().writeAndFlush(buf, backend.getChannel().voidPromise());
            } else {
                ReferenceCountUtil.release(msg);
            }
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    private void handleHandshake(Handshake handshake) {
        this.protocolVersion = handshake.getProtocolVersion();
        getPacketDecoder().setProtocolVersion(protocolVersion);
        getPacketEncoder().setProtocolVersion(protocolVersion);

        // FML 1.8+ appends a "\0FML\0" token (and possibly other NUL-delimited data) to the host.
        // Strip it for routing, and remember it so it can be restored on the backend handshake.
        String host = handshake.getHost();
        if (host.contains("\0")) {
            String[] split = host.split("\0", 2);
            this.requestedServer = split[0];
            this.extraDataInHandshake = "\0" + split[1];
        } else {
            this.requestedServer = host;
        }

        log.debug("Handshake from client: protocol={}, address={}, nextState={}, extraData={}",
                protocolVersion, requestedServer, handshake.getRequestedProtocol(),
                extraDataInHandshake.isEmpty() ? "-" : extraDataInHandshake);

        if (handshake.getRequestedProtocol() == 1) {
            setProtocol(Protocol.STATUS);
        } else if (handshake.getRequestedProtocol() == 2 || handshake.getRequestedProtocol() == 3) {
            // 2 = login, 3 = transfer (1.20.5+) — both proceed through the login phase.
            setProtocol(Protocol.LOGIN);
        } else {
            channel.close();
        }
    }

    private void handleStatusRequest() {
        proxy.getEventBus().postAsync(new ProxyPingEvent(protocolVersion, remoteIp()));
        String json = proxy.getPingResponseProvider().getPingJson(protocolVersion, proxy.getOnlineCount());
        channel.writeAndFlush(new StatusResponse(json));
    }

    private void handlePing(PingPacket ping) {
        channel.writeAndFlush(new PingPacket(ping.getPayload()));
    }

    /**
     * Terminates the pre-1.8 Forge (FML 1.7) handshake at the proxy with an empty mod list.
     */
    private void handleForgeHandshake(ForgeHandshake forgeHandshake) {
        if (!ForgeHandshake.FML_HANDSHAKE_CHANNEL.equals(forgeHandshake.getChannel())) {
            return;
        }
        byte[] data = forgeHandshake.getData();
        if (data == null || data.length == 0) {
            return;
        }
        byte discriminator = data[0];
        if (discriminator == 0) {
            // SERVERHELLO: echo it back.
            channel.writeAndFlush(new ForgeHandshake(ForgeHandshake.FML_HANDSHAKE_CHANNEL, new byte[]{0x00, 0x01}));
        } else if (discriminator == 2) {
            // Client MODLIST: respond with an empty server mod list.
            channel.writeAndFlush(new ForgeHandshake(ForgeHandshake.FML_HANDSHAKE_CHANNEL, new byte[]{0x02, 0x00}));
        } else if (discriminator == -1) {
            // ACK: respond with ACK.
            channel.writeAndFlush(new ForgeHandshake(ForgeHandshake.FML_HANDSHAKE_CHANNEL, new byte[]{(byte) 0xFF, 0x00}));
        }
    }

    private void handleLoginRequest(LoginRequest loginRequest) {
        if (loginInProgress) {
            return;
        }
        loginInProgress = true;
        this.username = loginRequest.getUsername();
        this.loginUuid = loginRequest.getUuid();

        if (proxy.getProperties().isOnlineMode()) {
            startEncryption();
        } else {
            finishLogin(offlineUuid(username), Collections.emptyList());
        }
    }

    private void startEncryption() {
        try {
            this.keyPair = generateKeyPair();
            this.verifyToken = generateVerifyToken();
            EncryptionRequest request = new EncryptionRequest("", keyPair.getPublic().getEncoded(), verifyToken);
            channel.writeAndFlush(request);
        } catch (Exception e) {
            log.error("Failed to start encryption for {}", username, e);
            channel.close();
        }
    }

    private void handleEncryptionResponse(EncryptionResponse response) {
        try {
            byte[] sharedSecret = rsaDecrypt(response.getSharedSecret(), keyPair.getPrivate());
            byte[] token = rsaDecrypt(response.getVerifyToken(), keyPair.getPrivate());
            if (!Arrays.equals(token, verifyToken)) {
                throw new IllegalStateException("verify token mismatch");
            }

            enableEncryption(sharedSecret);

            proxy.getMojangAuthService()
                    .authenticate(username, sharedSecret, keyPair.getPublic().getEncoded())
                    .thenAccept(profile -> channel.eventLoop().execute(
                            () -> finishLogin(profile.uuid(), profile.properties())))
                    .exceptionally(throwable -> {
                        log.error("Mojang authentication failed for {}", username, throwable);
                        channel.eventLoop().execute(channel::close);
                        return null;
                    });
        } catch (Exception e) {
            log.error("Failed to process encryption response for {}", username, e);
            channel.close();
        }
    }

    private void enableEncryption(byte[] sharedSecret) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(sharedSecret, "AES");
        Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(sharedSecret));
        Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
        decryptCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(sharedSecret));
        PipelineUtils.enableEncryption(channel.pipeline(), encryptCipher, decryptCipher);
    }

    private void finishLogin(UUID authenticatedUuid, List<LoginSuccess.Property> authenticatedProperties) {
        this.uuid = authenticatedUuid;
        this.properties = authenticatedProperties;

        this.user = new UserConnection(channel);
        user.setUsername(username);
        user.setUuid(authenticatedUuid);
        user.setLoginUuid(loginUuid);
        user.setProperties(authenticatedProperties);
        user.setProtocolVersion(protocolVersion);
        user.setRequestedServer(requestedServer);
        user.setExtraDataInHandshake(extraDataInHandshake);

        proxy.getEventBus().postAsync(
                new PlayerLoginEvent(username, authenticatedUuid, protocolVersion, requestedServer, remoteIp()));

        // Pause reading until the backend is ready to relay.
        channel.config().setAutoRead(false);
        new ServerConnector(proxy, user, this).connect();
    }

    /**
     * Called by the {@link ServerConnector} once the backend has accepted the login. Login Success
     * was already sent to the client by the ServerConnector, so this only swaps the client into the
     * play-phase relay.
     */
    public void onServerConnected(ServerConnection server) {
        this.server = server;

        setProtocol(Protocol.GAME);

        HandlerBoss boss = channel.pipeline().get(HandlerBoss.class);
        boss.setHandler(new UpstreamBridge(proxy, server));

        channel.config().setAutoRead(true);

        // Registers the player and broadcasts the TabList header/footer (with the live %online%)
        // to every connected player — including this one, whose front-end codecs were just
        // switched to GAME. This also covers backends that never send a header/footer of their
        // own (BungeeCord setTabHeader style).
        proxy.playerJoined(user);
        proxy.getEventBus().postAsync(
                new PlayerJoinEvent(username, uuid, protocolVersion, server.getHost(), server.getPort()));
        log.info("{} ({}) connected to backend {}:{}", username, uuid, server.getHost(), server.getPort());
    }

    private String remoteIp() {
        if (channel.remoteAddress() instanceof InetSocketAddress remote) {
            return remote.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private void setProtocol(Protocol newProtocol) {
        this.protocol = newProtocol;
        getPacketDecoder().setProtocol(newProtocol);
        getPacketEncoder().setProtocol(newProtocol);
    }

    private PacketDecoder getPacketDecoder() {
        return channel.pipeline().get(PacketDecoder.class);
    }

    private PacketEncoder getPacketEncoder() {
        return channel.pipeline().get(PacketEncoder.class);
    }

    private static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate RSA keypair", e);
        }
    }

    private static byte[] generateVerifyToken() {
        byte[] token = new byte[4];
        new SecureRandom().nextBytes(token);
        return token;
    }

    private static byte[] rsaDecrypt(byte[] data, PrivateKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (server != null && !server.isClosed()) {
            server.close();
        }
        proxy.getEventBus().postAsync(new PlayerDisconnectEvent(username, uuid));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.debug("InitialHandler exception: {}", cause.getMessage());
        ctx.close();
    }
}
