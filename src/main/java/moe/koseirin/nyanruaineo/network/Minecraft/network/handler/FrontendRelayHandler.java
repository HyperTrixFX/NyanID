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

@Slf4j
public class FrontendRelayHandler extends ChannelInboundHandlerAdapter {
    private final Channel backendChannel;

    public FrontendRelayHandler(Channel backendChannel) {
        this.backendChannel = backendChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (backendChannel.isActive()) {
            log.debug("FrontendRelay: forwarding packet {} to backend channel {}",
                    msg.getClass().getSimpleName(), backendChannel.id());
            backendChannel.writeAndFlush(msg);
        } else {
            log.warn("FrontendRelay: backend channel {} inactive, dropping packet", backendChannel.id());
            if (msg instanceof ByteBuf) {
                ((ByteBuf) msg).release();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Frontend channel {} is inactive, closing backend channel {}", ctx.channel().id(), backendChannel.id());
        if (backendChannel.isActive()) {
            backendChannel.close();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("FrontendRelay exception on channel {}", ctx.channel().id(), cause);
        ctx.close();
    }
}
