package moe.koseirin.nyanruaineo.network.Minecraft.service;



/*
 * @author KoseiRin_
 * awa
 */

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.PacketDecoder;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.PacketEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.BackendRelayHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.FrontendRelayHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.MinecraftProtocolHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.MinecraftPacketRegistry;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackendServerManager {

    private final ProxyProperties properties;
    private final MinecraftPacketRegistry packetRegistry;

    private final EventLoopGroup backendGroup = new NioEventLoopGroup(4);

    /**
     * 连接后端服务器，并在成功后发送初始包
     *
     * @param frontendChannel 前端客户端通道
     * @param initialPacket   需要首先发送到后端的包（如 LoginStartPacket）
     */
    public void connectAndForward(Channel frontendChannel, Packet initialPacket) {
        String host = properties.getBackendHost();
        int port = properties.getBackendPort();

        log.info("Attempting to connect to backend {}:{} for frontend channel {}", host, port, frontendChannel.id());

        // 暂停前端读取，防止在连接建立前有更多数据到达
        frontendChannel.config().setAutoRead(false);

        Bootstrap b = new Bootstrap();
        b.group(backendGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new PacketDecoder(packetRegistry, MinecraftProtocolHandler.STATE_KEY));
                        pipeline.addLast("encoder", new PacketEncoder());
                        pipeline.addLast("relay", new BackendRelayHandler(frontendChannel));
                        log.debug("Backend channel {} pipeline initialized", ch.id());
                    }
                });

        ChannelFuture future = b.connect(host, port);
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                Channel backendChannel = f.channel();
                log.info("Successfully connected to backend {}:{} (backend channel: {}) for frontend {}",
                        host, port, backendChannel.id(), frontendChannel.id());

                // 手动发送初始包（LoginStartPacket）
                sendPacket(backendChannel, initialPacket);
                log.debug("Initial packet (LoginStart) sent to backend channel {}", backendChannel.id());

                // 在前端 pipeline 添加转发处理器（注意添加位置在最后）
                if (frontendChannel.pipeline().get("frontendRelay") == null) {
                    frontendChannel.pipeline().addLast("frontendRelay", new FrontendRelayHandler(backendChannel));
                    log.debug("FrontendRelayHandler added to frontend channel {}", frontendChannel.id());
                }

                // 恢复前端读取
                frontendChannel.config().setAutoRead(true);
                log.debug("Frontend channel {} auto-read restored", frontendChannel.id());

                // 双向关闭监听
                backendChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.info("Backend channel {} closed, closing frontend {}", backendChannel.id(), frontendChannel.id());
                    if (frontendChannel.isActive()) {
                        frontendChannel.close();
                    }
                });
                frontendChannel.closeFuture().addListener((ChannelFutureListener) closeFuture -> {
                    log.info("Frontend channel {} closed, closing backend {}", frontendChannel.id(), backendChannel.id());
                    if (backendChannel.isActive()) {
                        backendChannel.close();
                    }
                });

            } else {
                log.error("Failed to connect to backend {}:{} for frontend {}", host, port, frontendChannel.id(), f.cause());
                frontendChannel.close();
            }
        });
    }

    /**
     * 将 Packet 编码并发送到指定通道
     */
    private void sendPacket(Channel channel, Packet packet) {
        ByteBuf temp = channel.alloc().buffer();
        try {
            VarIntCodec.writeVarInt(temp, packet.packetId());
            packet.encode(temp);
            log.trace("Encoding packet id {} for sending to channel {}", packet.packetId(), channel.id());
            channel.writeAndFlush(temp.retainedDuplicate());
        } finally {
            temp.release();
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down backend event loop group");
        backendGroup.shutdownGracefully();
    }
}
