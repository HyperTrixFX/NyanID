package moe.koseirin.nyanruaineo.network.Minecraft.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.HandlerBoss;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PacketCompressor;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PacketDecoder;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PacketDecompressor;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PacketEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PipelineUtils;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.ProtocolConstants;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.EncryptionRequest;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.EntityStatus;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.GameState;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.Handshake;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.JoinGame;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.Kick;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.LoginRequest;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.LoginSuccess;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.SetCompression;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.TabListHeaderFooter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.ViewDistance;
import moe.koseirin.nyanruaineo.network.Minecraft.service.BackendServer;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PlayerStateService;

import java.net.InetSocketAddress;

/**
 * Connects a logged-in client to the backend server and performs the login handshake, mirroring
 * BungeeCord's {@code ServerConnector}. On success it installs the play-phase bridges on both sides.
 */
@Slf4j
public class ServerConnector {

    private final MinecraftProxy proxy;
    private final UserConnection user;
    private final InitialHandler initialHandler;
    private final boolean serverSwitch;
    private final BackendServer backend;
    private final PlayerStateService playerState;

    public ServerConnector(MinecraftProxy proxy, UserConnection user, InitialHandler initialHandler) {
        this(proxy, user, initialHandler, null, false);
    }

    /**
     * Creates a connector for a cross-server switch to a specific backend. The Login Success is not
     * sent to the client again (it is already in the play phase).
     */
    public ServerConnector(MinecraftProxy proxy, UserConnection user, BackendServer target) {
        this(proxy, user, null, target, true);
    }

    private ServerConnector(MinecraftProxy proxy, UserConnection user, InitialHandler initialHandler,
                            BackendServer target, boolean serverSwitch) {
        this.proxy = proxy;
        this.user = user;
        this.initialHandler = initialHandler;
        this.serverSwitch = serverSwitch;
        this.backend = target != null ? target : proxy.getBackendServerManager().select(user.getRequestedServer());
        this.playerState = proxy.getPlayerStateService();
    }

    public void connect() {
        if (backend == null) {
            log.error("No backend servers configured for {} (check proxy.backend.servers)", user.getUsername());
            disconnectClient("{\"text\":\"No backend servers are configured\"}");
            return;
        }
        String host = backend.getHost();
        int port = backend.getPort();

        Bootstrap bootstrap = new Bootstrap()
                .group(proxy.getWorkerGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        PipelineUtils.initBackendPipeline(pipeline);
                        int generation = user.getServerGeneration();
                        ch.closeFuture().addListener(future -> {
                            log.debug("Backend channel closed for {}", user.getUsername());
                            // Only tear the client down when this backend is still the current one
                            // (a server switch bumps the generation).
                            if (user.getServerGeneration() == generation && user.getChannel().isActive()) {
                                user.getChannel().close();
                            }
                        });
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.error("Could not connect to backend {}:{} for {}", host, port, user.getUsername(), future.cause());
                disconnectClient("{\"text\":\"Could not connect to the backend server\"}");
                return;
            }

            Channel backendChannel = future.channel();
            ServerConnection server = new ServerConnection(backendChannel, host, port);
            server.setUser(user);
            // On a server switch the current server stays null until the new backend's JoinGame is
            // relayed, so the old bridge drops its stale packets and the client's own packets are
            // not forwarded into the still-logging-in backend.
            if (!serverSwitch) {
                user.setServer(server);
            }

            PacketDecoder decoder = backendChannel.pipeline().get(PacketDecoder.class);
            PacketEncoder encoder = backendChannel.pipeline().get(PacketEncoder.class);
            decoder.setProtocolVersion(user.getProtocolVersion());
            encoder.setProtocolVersion(user.getProtocolVersion());

            // 1. Handshake with the LOGIN intent, encoded in the HANDSHAKE state.
            Handshake handshake = new Handshake(user.getProtocolVersion(), buildForwardedHost(host), port, 2);
            backendChannel.writeAndFlush(handshake);

            // 2. Switch to LOGIN state and send the login request.
            decoder.setProtocol(Protocol.LOGIN);
            encoder.setProtocol(Protocol.LOGIN);
            LoginRequest loginRequest = new LoginRequest(user.getUsername());
            loginRequest.setUuid(user.getLoginUuid());
            backendChannel.writeAndFlush(loginRequest);

            // 3. Wait for the backend to accept (or reject) the login.
            HandlerBoss boss = backendChannel.pipeline().get(HandlerBoss.class);
            boss.setHandler(new LoginListener(backendChannel, server));
        });
    }

    /**
     * Sends a client-bound disconnect and closes the channel once the message is flushed.
     */
    private void disconnectClient(String reason) {
        if (!serverSwitch) {
            // Still in the LOGIN phase: a client-bound Kick (0x00) can be written directly.
            user.getChannel().writeAndFlush(new Kick(reason)).addListener(ChannelFutureListener.CLOSE);
        } else {
            // Already in the play phase after a failed switch: just close the connection.
            user.close();
        }
    }

    /**
     * Builds the Handshake host field, appending BungeeCord IP-forwarding data when enabled:
     * {@code host\0ip\0uuid\0propertiesJson}. When IP forwarding is disabled, the NUL-delimited
     * extra data stripped from the client's handshake (e.g. the FML 1.8+ token) is restored instead.
     */
    private String buildForwardedHost(String host) {
        if (proxy.getProperties().isIpForward()) {
            String ip = "127.0.0.1";
            if (user.getChannel().remoteAddress() instanceof InetSocketAddress remote) {
                ip = remote.getAddress().getHostAddress();
            }

            JSONArray properties = new JSONArray();
            for (LoginSuccess.Property property : user.getProperties()) {
                JSONObject prop = new JSONObject();
                prop.put("name", property.getName());
                prop.put("value", property.getValue());
                prop.put("signature", property.getSignature() == null ? "" : property.getSignature());
                properties.add(prop);
            }

            return host + "\0" + ip + "\0" + user.getUuid() + "\0" + properties.toJSONString();
        }

        String extra = user.getExtraDataInHandshake();
        return host + (extra == null ? "" : extra);
    }

    /**
     * Temporary backend handler that watches for the login result, then swaps in the play-phase
     * bridge.
     */
    private final class LoginListener extends ChannelInboundHandlerAdapter {

        private final Channel backendChannel;
        private final ServerConnection server;

        private LoginListener(Channel backendChannel, ServerConnection server) {
            this.backendChannel = backendChannel;
            this.server = server;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof LoginSuccess) {
                onLoginSuccess();
            } else if (msg instanceof JoinGame joinGame) {
                onJoinGame(joinGame);
            } else if (msg instanceof Kick kick) {
                log.warn("Login denied by backend for {}", user.getUsername());
                disconnectClient(kick.getReason());
            } else if (msg instanceof SetCompression setCompression) {
                enableCompression(setCompression.getThreshold());
            } else if (msg instanceof EncryptionRequest) {
                log.error("Backend {}:{} requested encryption (online mode). The backend must run in "
                        + "offline mode (online-mode=false) so the proxy can complete login.",
                        server.getHost(), server.getPort());
                disconnectClient("{\"text\":\"Backend server is misconfigured (online mode)\"}");
            } else if (msg instanceof io.netty.buffer.ByteBuf buf) {
                // Unknown login-state packet from the backend (e.g. login plugin request): relay it
                // to the client verbatim.
                Channel userChannel = user.getChannel();
                if (userChannel.isActive()) {
                    userChannel.writeAndFlush(buf, userChannel.voidPromise());
                } else {
                    ReferenceCountUtil.release(msg);
                }
            } else {
                ReferenceCountUtil.release(msg);
            }
        }

        private void enableCompression(int threshold) {
            if (backendChannel.pipeline().get(ProtocolConstants.COMPRESSION_ENCODER) != null) {
                return;
            }
            // Inbound: frame-decoder -> decompressor -> packet-decoder.
            backendChannel.pipeline().addAfter(ProtocolConstants.FRAME_DECODER,
                    ProtocolConstants.COMPRESSION_DECODER, new PacketDecompressor());
            // Outbound: packet-encoder -> compressor -> frame-prepender.
            backendChannel.pipeline().addAfter(ProtocolConstants.FRAME_PREPENDER,
                    ProtocolConstants.COMPRESSION_ENCODER, new PacketCompressor(threshold));
            log.debug("Backend {}:{} enabled compression with threshold {}", server.getHost(), server.getPort(), threshold);
        }

        private void onLoginSuccess() {
            // Switch the backend to the play-phase protocol. The bridge is deliberately NOT
            // installed yet: the JoinGame packet is intercepted below and relayed to the client
            // first (BungeeCord cut-through), so no backend traffic can precede the world reset.
            backendChannel.pipeline().get(PacketDecoder.class).setProtocol(Protocol.GAME);
            backendChannel.pipeline().get(PacketEncoder.class).setProtocol(Protocol.GAME);

            if (serverSwitch) {
                return;
            }

            // First connection: send Login Success to the client. Pre-1.19.3 backends emit PLAY
            // packets immediately after Login Success, so it must be written before any backend
            // traffic is relayed.
            LoginSuccess loginSuccess = new LoginSuccess(user.getUuid(), user.getUsername(), user.getProperties());
            if (log.isDebugEnabled()) {
                io.netty.buffer.ByteBuf probe = io.netty.buffer.Unpooled.buffer();
                try {
                    loginSuccess.write(probe, user.getProtocolVersion());
                    byte[] bytes = new byte[probe.readableBytes()];
                    probe.readBytes(bytes);
                    log.debug("LoginSuccess wire bytes ({} bytes, protocol {}): {}", bytes.length,
                            user.getProtocolVersion(), java.util.HexFormat.of().formatHex(bytes));
                } finally {
                    probe.release();
                }
            }
            user.sendPacket(loginSuccess);
            log.debug("LoginSuccess sent to {} ({}) for protocol {}", user.getUsername(), user.getUuid(),
                    user.getProtocolVersion());
        }

        /**
         * BungeeCord cut-through: mirrors {@code ServerConnector.handleLogin} + {@code cutThrough}.
         * First connections (any version) and 1.16+ switches get the JoinGame written directly to
         * the client, followed (on a switch) by a Respawn with the new server's world data and the
         * removal of everything the old server sent. Pre-1.16 switches perform the legacy respawn
         * dance instead of forwarding the JoinGame. The play-phase bridge is only installed after
         * the world reset, so no stale packet can precede or corrupt it.
         */
        private void onJoinGame(JoinGame login) {
            int version = user.getProtocolVersion();

            playerState.setClientEntityId(user, login.getEntityId());
            playerState.setServerEntityId(user, login.getEntityId());

            // First connection (any version) or 1.16+ switch: the JoinGame is sent directly.
            if (!serverSwitch || version >= 735) {
                if (!serverSwitch) {
                    // Complete the client side first (front-end codecs switch to GAME and the
                    // UpstreamBridge is installed); the JoinGame write below is submitted
                    // afterwards, so it is encoded in the GAME state.
                    user.getChannel().eventLoop().execute(() -> initialHandler.onServerConnected(server));
                }

                user.sendPacket(login);
                if (serverSwitch) {
                    // BungeeCord handleLogin: forget the old server's scoreboard/teams/bossbars,
                    // reset the tab list names, then rebuild the world state with a Respawn.
                    playerState.clearServerState(user, user::sendPacket);
                    proxy.getTabListService().resetTabList(user);
                    user.sendPacket(login.toRespawn());
                }
                playerState.setDimension(user, login.getDimension());
            } else {
                // Pre-1.16 switch: the legacy respawn dance (no JoinGame forwarded).
                playerState.clearServerState(user, user::sendPacket);
                proxy.getTabListService().resetTabList(user);
                user.sendPacket(new EntityStatus(playerState.getClientEntityId(user),
                        login.isReducedDebugInfo() ? EntityStatus.DEBUG_INFO_REDUCED : EntityStatus.DEBUG_INFO_NORMAL));
                if (version >= 573) {
                    user.sendPacket(new GameState(GameState.IMMEDIATE_RESPAWN, login.isNormalRespawn() ? 0 : 1));
                }
                playerState.setDimensionChange(user, true);
                Object currentDimension = playerState.getDimension(user);
                if (currentDimension != null && login.getDimension() != null
                        && login.getDimension().equals(currentDimension)) {
                    user.sendPacket(login.toRespawn(((Integer) login.getDimension()) >= 0 ? -1 : 0));
                }
                playerState.setServerEntityId(user, login.getEntityId());
                user.sendPacket(login.toRespawn());
                if (version >= 477) {
                    user.sendPacket(new ViewDistance(login.getViewDistance()));
                }
                playerState.setDimension(user, login.getDimension());
            }

            // cutThrough: swap the current server, then install the play-phase bridge.
            user.setServer(server);
            HandlerBoss boss = backendChannel.pipeline().get(HandlerBoss.class);
            boss.setHandler(new DownstreamBridge(proxy, user, server));

            if (serverSwitch) {
                user.getChannel().eventLoop().execute(() -> {
                    user.getChannel().config().setAutoRead(true);
                    sendTabHeaderFooter();
                });
                log.debug("{} ({}) switched to backend {}:{}", user.getUsername(), user.getUuid(),
                        server.getHost(), server.getPort());
            }
        }

        /**
         * Sends the configured TabList header/footer to the client when enabled. Used on server
         * switches, where the front-end codecs are already in the GAME state. Backend-sent
         * header/footer packets are normalised by the TabList interception in DownstreamBridge.
         */
        private void sendTabHeaderFooter() {
            TabListHeaderFooter tabHeader = proxy.getTabListService()
                    .buildHeaderFooter(user, proxy.getOnlineCount());
            if (tabHeader != null) {
                user.sendPacket(tabHeader);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Backend login error for {}", user.getUsername(), cause);
            user.close();
        }
    }
}
