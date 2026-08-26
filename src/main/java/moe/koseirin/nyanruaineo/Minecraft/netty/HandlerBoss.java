package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.Getter;
import lombok.Setter;

/**
 * 将入站通道事件委托给一个可替换的 {@link ChannelInboundHandler}。
 * 用于在初始握手/登录处理器和游戏阶段桥接器之间切换逻辑前端处理器。
 */
@Setter
@Getter
public class HandlerBoss extends ChannelInboundHandlerAdapter {

    private ChannelInboundHandler handler;

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
