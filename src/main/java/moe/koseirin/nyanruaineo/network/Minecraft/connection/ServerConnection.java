package moe.koseirin.nyanruaineo.network.Minecraft.connection;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;

/**
 * The server-facing half of a proxied connection, mirroring BungeeCord's {@code ServerConnection}.
 */
@Getter
public class ServerConnection extends Connection {

    private final String host;
    private final int port;
    @Setter
    private UserConnection user;

    public ServerConnection(Channel channel, String host, int port) {
        super(channel);
        this.host = host;
        this.port = port;
    }

}
