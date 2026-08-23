package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * Delegates inbound channel events to a swappable {@link ChannelInboundHandler}. Used to switch
 * the logical front handler between the initial handshake/login handler and the play-phase
 * bridges. Mirrors BungeeCord's {@code HandlerBoss}.
 */
public class HandlerBoss extends ChannelInboundHandlerAdapter {

    private ChannelInboundHandler handler;

    public ChannelInboundHandler getHandler() {
        return handler;
    }

    public void setHandler(ChannelInboundHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (handler != null) {
            handler.channelActive(ctx);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (handler != null) {
            handler.channelRead(ctx, msg);
        } else {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (handler != null) {
            handler.channelInactive(ctx);
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (handler != null) {
            handler.channelWritabilityChanged(ctx);
        } else {
            ctx.fireChannelWritabilityChanged();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (handler != null) {
            handler.exceptionCaught(ctx, cause);
        } else {
            ctx.close();
        }
    }
}
