package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import moe.koseirin.nyanruaineo.Minecraft.service.FirewallService;

import java.net.InetSocketAddress;

/**
 * 前端管道的第一个入站处理器：在连接建立时检查防火墙，被拒则立即关闭连接；连接关闭时释放并发额度。
 */
public class FirewallHandler extends ChannelInboundHandlerAdapter {

    private final FirewallService firewall;
    private InetSocketAddress remoteAddress;
    private boolean accepted;

    public FirewallHandler(FirewallService firewall) {
        this.firewall = firewall;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().remoteAddress() instanceof InetSocketAddress address) {
            this.remoteAddress = address;
            this.accepted = firewall.tryConnect(address);
        } else {
            this.accepted = true;
        }
        if (!accepted) {
            ctx.close();
            return;
        }
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (accepted && remoteAddress != null) {
            firewall.onDisconnect(remoteAddress);
            accepted = false;
        }
        ctx.fireChannelInactive();
    }
}
