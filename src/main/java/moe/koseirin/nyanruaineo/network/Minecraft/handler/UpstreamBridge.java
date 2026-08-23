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
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerCommandEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerDisconnectEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.Chat;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.ClientChat;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.ClientCommand;

/**
 * Relays client-to-server traffic to the backend, mirroring BungeeCord's {@code UpstreamBridge}.
 * Installed on the front-end channel once the play phase begins. Decodes chat/command packets,
 * fires {@link PlayerCommandEvent} so proxy listeners can consume commands first, and forwards
 * everything that was not consumed.
 */
@Slf4j
public class UpstreamBridge extends ChannelInboundHandlerAdapter {

    private final MinecraftProxy proxy;
    private final UserConnection user;

    public UpstreamBridge(MinecraftProxy proxy, ServerConnection server) {
        this.proxy = proxy;
        this.user = server.getUser();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ServerConnection server = user.getServer();
        if (server == null || !server.getChannel().isActive()) {
            ReferenceCountUtil.release(msg);
            return;
        }

        // BungeeCord-style command interception: the chat/command packets are decoded by the
        // pipeline; the proxy matches commands first, and only unhandled ones are forwarded.
        if (msg instanceof Chat chat) {
            String message = chat.getMessage().trim();
            if (message.startsWith("/") && handleCommand(message)) {
                log.debug("Command handled by proxy: {} issued '{}'", user.getUsername(), message);
                return;
            }
            server.getChannel().writeAndFlush(chat, server.getChannel().voidPromise());
            return;
        }

        if (msg instanceof ClientCommand command) {
            // The 1.19+ command field carries no leading slash (BungeeCord prepends it).
            String full = "/" + command.getCommand().trim();
            if (handleCommand(full)) {
                log.debug("Command handled by proxy: {} issued '{}'", user.getUsername(), full);
                return;
            }
            server.getChannel().writeAndFlush(command, server.getChannel().voidPromise());
            return;
        }

        if (msg instanceof ClientChat chat) {
            String message = chat.getMessage().trim();
            if (message.startsWith("/") && handleCommand(message)) {
                log.debug("Command handled by proxy: {} issued '{}'", user.getUsername(), message);
                return;
            }
            server.getChannel().writeAndFlush(chat, server.getChannel().voidPromise());
            return;
        }

        if (log.isDebugEnabled() && msg instanceof ByteBuf buf && buf.isReadable()) {
            try {
                int packetId = DefinedPacket.readVarInt(buf.duplicate());
                log.debug("Upstream client -> {}: packetId=0x{}, bytes={}", server.getHost(),
                        Integer.toHexString(packetId), buf.readableBytes());
            } catch (Exception ignored) {
                // Never fail forwarding because of a diagnostic read.
            }
        }
        server.getChannel().writeAndFlush(msg, server.getChannel().voidPromise());
    }

    /**
     * Dispatches a player command. The {@link PlayerCommandEvent} is fired synchronously so
     * listeners (e.g. {@code ProxyEventListener#onPlayerCommand}) can handle and cancel it; only
     * commands no listener consumed are forwarded to the backend.
     *
     * @return true when the command was consumed by the proxy (do not forward).
     */
    private boolean handleCommand(String message) {
        String[] parts = message.split(" ", 2);
        String command = parts[0];
        String args = parts.length > 1 ? parts[1].trim() : "";

        PlayerCommandEvent event = new PlayerCommandEvent(user, command, args, this::sendChatMessage);
        proxy.getEventBus().post(event);
        return event.isCancelled();
    }

    /** Sends a chat message back to this player (delegates to the shared message service). */
    private void sendChatMessage(String text) {
        proxy.getPlayerMessageService().sendMessage(user, text);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ServerConnection server = user.getServer();
        if (server != null && !server.isClosed()) {
            server.close();
        }
        proxy.playerLeft(user);
        proxy.getEventBus().postAsync(new PlayerDisconnectEvent(user.getUsername(), user.getUuid()));
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
        ServerConnection server = user.getServer();
        Channel serverChannel = server == null ? null : server.getChannel();
        if (serverChannel != null) {
            // Stop reading from the backend when the client cannot keep up.
            serverChannel.config().setAutoRead(ctx.channel().isWritable());
        }
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }
}
