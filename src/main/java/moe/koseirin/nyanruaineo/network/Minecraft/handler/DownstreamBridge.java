package moe.koseirin.nyanruaineo.network.Minecraft.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerListItem;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.TabListHeaderFooter;

/**
 * Relays server-to-client traffic to the client, mirroring BungeeCord's {@code DownstreamBridge}.
 * Installed on the back-end channel once the play phase begins. The TabList packets decoded by
 * the pipeline are passed through the {@code TabListService} for modification before relaying;
 * everything else is forwarded as-is.
 */
@Slf4j
public class DownstreamBridge extends ChannelInboundHandlerAdapter {

    private final MinecraftProxy proxy;
    private final UserConnection user;
    private final int generation;

    public DownstreamBridge(MinecraftProxy proxy, UserConnection user) {
        this.proxy = proxy;
        this.user = user;
        this.generation = user.getServerGeneration();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel userChannel = user.getChannel();
        if (!userChannel.isActive()) {
            ReferenceCountUtil.release(msg);
            return;
        }

        // TabList interception: the header/footer is replaced with the configured text and player
        // entry display names are wrapped in the configured prefix/suffix (when enabled).
        if (msg instanceof TabListHeaderFooter headerFooter) {
            proxy.getTabListService().applyHeaderFooter(headerFooter, proxy.getOnlineCount());
        } else if (msg instanceof PlayerListItem playerListItem) {
            proxy.getTabListService().decorate(playerListItem, user);
        }

        if (log.isDebugEnabled() && msg instanceof ByteBuf buf && buf.isReadable()) {
            try {
                int packetId = DefinedPacket.readVarInt(buf.duplicate());
                log.debug("Downstream {} -> client: packetId=0x{}, bytes={}", user.getUsername(),
                        Integer.toHexString(packetId), buf.readableBytes());
            } catch (Exception ignored) {
                // Never fail forwarding because of a diagnostic read.
            }
        }
        userChannel.writeAndFlush(msg, userChannel.voidPromise());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // Only tear the client down when this backend is still the current one (a server switch
        // bumps the generation and a new backend takes over).
        if (user.getServerGeneration() == generation) {
            user.close();
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        Channel userChannel = user.getChannel();
        if (userChannel != null) {
            // Stop reading from the client when the backend cannot keep up.
            userChannel.config().setAutoRead(ctx.channel().isWritable());
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
