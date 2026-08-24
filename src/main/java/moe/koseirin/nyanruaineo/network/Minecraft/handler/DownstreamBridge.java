package moe.koseirin.nyanruaineo.network.Minecraft.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.BossBar;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerInfoRemove;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerInfoUpdate;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerListItem;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.ScoreboardObjective;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.ScoreboardScore;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.TabListHeaderFooter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.Team;

/**
 * Relays server-to-client traffic to the client, mirroring BungeeCord's {@code DownstreamBridge}.
 * Installed on the back-end channel once the play phase begins. The TabList packets decoded by
 * the pipeline are passed through the {@code TabListService} for modification before relaying;
 * everything else is forwarded as-is. Packets are dropped as soon as the user switched to another
 * backend, so the old server's trailing packets can never leak into the new world
 * (BungeeCord's {@code con.getServer() == this.server} guard).
 */
@Slf4j
public class DownstreamBridge extends ChannelInboundHandlerAdapter {

    private final MinecraftProxy proxy;
    private final UserConnection user;
    private final ServerConnection server;
    private final int generation;

    public DownstreamBridge(MinecraftProxy proxy, UserConnection user, ServerConnection server) {
        this.proxy = proxy;
        this.user = user;
        this.server = server;
        this.generation = user.getServerGeneration();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel userChannel = user.getChannel();
        if (!userChannel.isActive()) {
            ReferenceCountUtil.release(msg);
            return;
        }
        // Stale-backend guard: once a server switch pointed the user elsewhere, anything still in
        // flight from this (now old) backend is dropped instead of corrupting the new world.
        if (user.getServer() != server) {
            ReferenceCountUtil.release(msg);
            return;
        }

        // TabList interception: the header/footer is replaced with the configured text and player
        // entry display names are wrapped in the configured prefix/suffix (when enabled).
        if (msg instanceof TabListHeaderFooter headerFooter) {
            proxy.getTabListService().applyHeaderFooter(headerFooter, user, proxy.getOnlineCount());
        } else if (msg instanceof PlayerListItem playerListItem) {
            proxy.getTabListService().decorate(playerListItem, user, proxy.getOnlineUsers());
        } else if (msg instanceof PlayerInfoUpdate playerInfoUpdate) {
            proxy.getTabListService().decorateUpdate(playerInfoUpdate, user, proxy.getOnlineUsers());
        } else if (msg instanceof PlayerInfoRemove playerInfoRemove) {
            proxy.getTabListService().removeEntries(playerInfoRemove, user);
        }

        // Server-side state tracking (BungeeCord DownstreamBridge parity): the switch cleanup
        // needs to know which scoreboard objectives/scores/teams/bossbars this server sent.
        if (msg instanceof ScoreboardObjective objective) {
            proxy.getPlayerStateService().trackObjective(user, objective.getName(), objective.getAction() != 1);
        } else if (msg instanceof ScoreboardScore score) {
            proxy.getPlayerStateService().trackScore(user, score.getItemName(), score.getScoreName(), score.getAction() != 1);
        } else if (msg instanceof Team team) {
            proxy.getPlayerStateService().trackTeam(user, team.getName(), team.getMode() != 1);
        } else if (msg instanceof BossBar bossBar) {
            proxy.getPlayerStateService().trackBossBar(user, bossBar.getUuid(), bossBar.getAction() != 1);
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
