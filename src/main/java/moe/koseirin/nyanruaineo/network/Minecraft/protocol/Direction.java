package moe.koseirin.nyanruaineo.network.Minecraft.protocol;

/**
 * Packet direction, mirroring BungeeCord's {@code ProtocolConstants.Direction}.
 */
public enum Direction {

    /**
     * Packets flowing from a client towards a server (clientbound-in-bound, i.e. inbound on the
     * front-end channel and outbound on the back-end channel).
     */
    TO_SERVER,

    /**
     * Packets flowing from a server towards a client (outbound on the front-end channel and
     * inbound on the back-end channel).
     */
    TO_CLIENT
}
