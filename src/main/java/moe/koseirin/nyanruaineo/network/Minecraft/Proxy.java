package moe.koseirin.nyanruaineo.network.Minecraft;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.PacketDecoder;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.PacketEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.MinecraftProtocolHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.MinecraftPacketRegistry;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Proxy {

    private final ProxyProperties properties;

    private final MinecraftPacketRegistry minecraftPacketRegistry;

    private final MinecraftProtocolHandler protocolHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    public Proxy(ProxyProperties properties, MinecraftPacketRegistry minecraftPacketRegistry, MinecraftProtocolHandler protocolHandler) {
        this.properties = properties;
        this.minecraftPacketRegistry = minecraftPacketRegistry;
        this.protocolHandler = protocolHandler;
    }

    @PostConstruct
    public void start() throws InterruptedException {
        int port = properties.getPort();
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast("packetDecoder", new PacketDecoder(minecraftPacketRegistry, MinecraftProtocolHandler.STATE_KEY));

                        p.addLast("packetEncoder", new PacketEncoder());
                        p.addLast("protocolHandler", protocolHandler);
                    }
                });

        channelFuture = b.bind(port).sync();
        log.info("Minecraft proxy started on port {}", port);
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping Minecraft proxy...");
        if (channelFuture != null) {
            channelFuture.channel().close().syncUninterruptibly();
        }
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        log.info("Minecraft proxy stopped.");
    }
}
