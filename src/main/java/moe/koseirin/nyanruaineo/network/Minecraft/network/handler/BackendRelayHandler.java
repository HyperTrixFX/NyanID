package moe.koseirin.nyanruaineo.network.Minecraft.network.handler;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginSuccessPacket;


@Slf4j
public class BackendRelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel frontendChannel;

    public BackendRelayHandler(Channel frontendChannel) {
        this.frontendChannel = frontendChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof LoginSuccessPacket) {
            log.debug("Ignoring LoginSuccess from backend for channel {}", frontendChannel.id());
            return;
        }

        if (frontendChannel.isActive()) {
            frontendChannel.writeAndFlush(msg);
        } else {
            if (msg instanceof ByteBuf) {
                ((ByteBuf) msg).release();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Backend channel {} is inactive, closing frontend channel {}", ctx.channel().id(), frontendChannel.id());
        if (frontendChannel.isActive()) {
            frontendChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("BackendRelay exception on channel {} cause {}", ctx.channel().id(), cause.getMessage());
        ctx.close();
    }
}
