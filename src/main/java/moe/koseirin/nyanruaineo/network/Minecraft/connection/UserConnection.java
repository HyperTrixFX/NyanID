package moe.koseirin.nyanruaineo.network.Minecraft.connection;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.LoginSuccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The client-facing half of a proxied connection, mirroring BungeeCord's {@code UserConnection}.
 */
@Getter
public class UserConnection extends Connection {

    @Setter
    private String username;
    @Setter
    private UUID uuid;
    /**
     * -- GETTER --
     *  The UUID the client declared in its login start packet (only present for 1.20.5+).
     */
    @Setter
    private UUID loginUuid;
    private List<LoginSuccess.Property> properties = new ArrayList<>();
    @Setter
    private int protocolVersion;
    /**
     * -- GETTER --
     *  The server address the client requested in its Handshake, used for backend routing.
     */
    @Setter
    private String requestedServer;
    /**
     * -- GETTER --
     *  NUL-delimited extra data stripped from the handshake host (e.g. the FML 1.8+ token). Restored
     *  on the backend handshake when IP forwarding is disabled.
     */
    @Setter
    private String extraDataInHandshake = "";
    @Setter
    private ServerConnection server;
    private int serverGeneration;
    /**
     * Player names seen in TabList ADD_PLAYER packets, used to resolve the names of
     * UPDATE_DISPLAY_NAME entries during the TabList prefix/suffix interception.
     */
    private final Map<UUID, String> tabListNames = new HashMap<>();

    public UserConnection(Channel channel) {
        super(channel);
    }

    /**
     * Bumps the server connection generation. Close listeners bound to a previous generation must
     * not tear the client down once a server switch started.
     */
    public void nextServerGeneration() {
        this.serverGeneration++;
    }

    public void setProperties(List<LoginSuccess.Property> properties) {
        this.properties = properties == null ? new ArrayList<>() : properties;
    }

}
