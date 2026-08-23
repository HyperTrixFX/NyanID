package moe.koseirin.nyanruaineo.network.Minecraft.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet registry for a single {@link Protocol} state and {@link Direction}, mirroring BungeeCord's
 * {@code ProtocolData}. Packets may be registered for all protocol versions or for a version range;
 * the decoder and encoder resolve by packet id (or class) plus the negotiated protocol version.
 */
public final class ProtocolData {

    private static final int ANY_VERSION = Integer.MAX_VALUE;

    private final List<Entry> entries = new ArrayList<>();

    private record Entry(int minVersion, int maxVersion, int packetId,
                         Class<? extends DefinedPacket> packetClass,
                         Supplier<? extends DefinedPacket> constructor) {
    }

    /** Registers a packet for every protocol version. */
    public <T extends DefinedPacket> void register(int packetId, Class<T> packetClass, Supplier<T> constructor) {
        register(0, ANY_VERSION, packetId, packetClass, constructor);
    }

    /** Registers a packet for the inclusive protocol version range. */
    public <T extends DefinedPacket> void register(int minVersion, int maxVersion, int packetId,
                                                   Class<T> packetClass, Supplier<T> constructor) {
        entries.add(new Entry(minVersion, maxVersion, packetId, packetClass, constructor));
    }

    private static boolean inRange(Entry entry, int protocolVersion) {
        // A negative (unknown) version matches any registration: the handshake packet is decoded
        // before the real protocol version is known.
        return protocolVersion < 0
                || (protocolVersion >= entry.minVersion && protocolVersion <= entry.maxVersion);
    }

    /**
     * Creates a fresh packet instance for the given id and protocol version, or {@code null} when
     * the id is not registered in this state/direction/version (the decoder then falls back to raw
     * passthrough).
     */
    public DefinedPacket createPacket(int packetId, int protocolVersion) {
        for (Entry entry : entries) {
            if (entry.packetId == packetId && inRange(entry, protocolVersion)) {
                return entry.constructor.get();
            }
        }
        return null;
    }

    public boolean hasPacket(int packetId, int protocolVersion) {
        return createPacket(packetId, protocolVersion) != null;
    }

    public int getId(Class<? extends DefinedPacket> packetClass, int protocolVersion) {
        for (Entry entry : entries) {
            if (entry.packetClass == packetClass && inRange(entry, protocolVersion)) {
                return entry.packetId;
            }
        }
        throw new IllegalArgumentException("Packet not registered for this state/direction/version: "
                + packetClass.getSimpleName() + " (" + protocolVersion + ")");
    }
}
