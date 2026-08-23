package moe.koseirin.nyanruaineo.network.Minecraft.connection;

import io.netty.channel.Channel;
import lombok.Getter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Base for a proxy-side network connection, mirroring BungeeCord's {@code Connection}.
 */
@Getter
public abstract class Connection {

    private final Channel channel;

    protected Connection(Channel channel) {
        this.channel = channel;
    }

    public boolean isClosed() {
        return !channel.isActive();
    }

    /**
     * Writes a packet through this connection's pipeline. The channel's outbound encoder resolves
     * the packet id from the packet class.
     */
    public void sendPacket(DefinedPacket packet) {
        channel.writeAndFlush(packet);
    }

    public void close() {
        if (channel.isActive()) {
            channel.close();
        }
    }
}
