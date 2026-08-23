package moe.koseirin.nyanruaineo.network.Minecraft;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.EventBus;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.handler.InitialHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.HandlerBoss;
import moe.koseirin.nyanruaineo.network.Minecraft.netty.PipelineUtils;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.TabListHeaderFooter;
import moe.koseirin.nyanruaineo.network.Minecraft.service.BackendServerManager;
import moe.koseirin.nyanruaineo.network.Minecraft.service.MojangAuthService;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PingResponseProvider;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PlayerMessageService;
import moe.koseirin.nyanruaineo.network.Minecraft.service.TabListService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The Minecraft proxy server bootstrap, mirroring BungeeCord's proxy startup: it owns the Netty
 * event loops, binds the front-end listener and wires each accepted channel into the
 * handshake/login pipeline.
 */
@Slf4j
@Component
public class MinecraftProxy {

    @Getter
    private final ProxyProperties properties;
    @Getter
    private final MojangAuthService mojangAuthService;
    @Getter
    private final PingResponseProvider pingResponseProvider;
    @Getter
    private final BackendServerManager backendServerManager;
    @Getter
    private final EventBus eventBus;
    @Getter
    private final TabListService tabListService;
    @Getter
    private final PlayerMessageService playerMessageService;

    @Value("${NyanidSetting.EnableProxy:false}")
    private boolean enableProxy;

    private EventLoopGroup bossGroup;
    @Getter
    private EventLoopGroup workerGroup;
    private ChannelFuture serverFuture;

    /** Number of players currently in the play phase (used for the MOTD online count). */
    private final java.util.concurrent.atomic.AtomicInteger onlineCount = new java.util.concurrent.atomic.AtomicInteger();

    /** The players currently in the play phase, used to broadcast live TabList updates. */
    @Getter
    private final java.util.Set<UserConnection> onlineUsers = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public MinecraftProxy(ProxyProperties properties,
                          MojangAuthService mojangAuthService,
                          PingResponseProvider pingResponseProvider,
                          BackendServerManager backendServerManager,
                          EventBus eventBus,
                          TabListService tabListService,
                          PlayerMessageService playerMessageService) {
        this.properties = properties;
        this.mojangAuthService = mojangAuthService;
        this.pingResponseProvider = pingResponseProvider;
        this.backendServerManager = backendServerManager;
        this.eventBus = eventBus;
        this.tabListService = tabListService;
        this.playerMessageService = playerMessageService;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        if (!enableProxy) {
            log.info("Minecraft proxy is disabled");
            return;
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        HandlerBoss boss = PipelineUtils.initFrontendPipeline(ch.pipeline());
                        boss.setHandler(new InitialHandler(MinecraftProxy.this, ch));
                    }
                });

        serverFuture = bootstrap.bind(properties.getPort()).sync();
        log.info("Minecraft proxy started on port {}", properties.getPort());
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Minecraft proxy...");
        if (serverFuture != null) {
            serverFuture.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Minecraft proxy stopped.");
    }

    /** Called once a player reached the play phase. */
    public void playerJoined(UserConnection user) {
        onlineUsers.add(user);
        onlineCount.incrementAndGet();
        refreshTabList();
    }

    /** Called once a play-phase player disconnects. */
    public void playerLeft(UserConnection user) {
        onlineUsers.remove(user);
        onlineCount.updateAndGet(value -> Math.max(0, value - 1));
        refreshTabList();
    }

    /** The current number of online (play-phase) players. */
    public int getOnlineCount() {
        return onlineCount.get();
    }

    /**
     * Broadcasts a chat message to every connected player (each in their own version's chat
     * format) and returns the number of recipients.
     */
    public int broadcast(String text) {
        return playerMessageService.broadcast(onlineUsers, text);
    }

    /**
     * Re-pushes the TabList header/footer to every connected player whenever the online count
     * changed, so placeholders like {@code %online%} stay live for everyone.
     */
    private void refreshTabList() {
        int count = onlineCount.get();
        for (UserConnection player : onlineUsers) {
            try {
                if (player.getChannel() == null || !player.getChannel().isActive()) {
                    onlineUsers.remove(player);
                    continue;
                }
                TabListHeaderFooter header = tabListService.buildHeaderFooter(player.getProtocolVersion(), count);
                if (header != null) {
                    player.sendPacket(header);
                }
            } catch (Exception e) {
                log.warn("Failed to refresh the TabList for {}", player.getUsername(), e);
            }
        }
    }
}
