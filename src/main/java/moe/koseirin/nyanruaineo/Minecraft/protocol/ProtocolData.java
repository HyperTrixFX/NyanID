package moe.koseirin.nyanruaineo.Minecraft.protocol;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 单个 {@link Protocol} 阶段和 {@link Direction} 的数据包注册表。
 * 数据包可注册为适用于所有协议版本或某个版本范围。
 * 查找操作会一次性构建每个版本的哈希映射（连接在握手后其协议版本不再改变），
 * 因此热路径上的解码/编码为 O(1) 而非遍历所有注册项。
 */
public final class ProtocolData {

    private static final int ANY_VERSION = Integer.MAX_VALUE;

    private final List<Entry> entries = new ArrayList<>();

    private volatile int cachedVersion = Integer.MIN_VALUE;
    private volatile Map<Integer, Entry> idLookup = Map.of();
    private volatile Map<Class<? extends DefinedPacket>, Integer> classLookup = Map.of();

    public record Entry(int minVersion, int maxVersion, int packetId,
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

    /** O(1) lookup of the registration for the given id and version, or {@code null}. */
    public Entry getEntry(int packetId, int protocolVersion) {
        return idLookup(protocolVersion).get(packetId);
    }

    /**
     * Creates a fresh packet instance for the given id and protocol version, or {@code null} when
     * the id is not registered in this state/direction/version (the decoder then falls back to raw
     * passthrough).
     */
    public DefinedPacket createPacket(int packetId, int protocolVersion) {
        Entry entry = getEntry(packetId, protocolVersion);
        return entry == null ? null : entry.constructor().get();
    }

    public boolean hasPacket(int packetId, int protocolVersion) {
        return getEntry(packetId, protocolVersion) != null;
    }

    public int getId(Class<? extends DefinedPacket> packetClass, int protocolVersion) {
        Integer packetId = classLookup(protocolVersion).get(packetClass);
        if (packetId == null) {
            throw new IllegalArgumentException("Packet not registered for this state/direction/version: "
                    + packetClass.getSimpleName() + " (" + protocolVersion + ")");
        }
        return packetId;
    }

    private Map<Integer, Entry> idLookup(int protocolVersion) {
        if (protocolVersion == cachedVersion) {
            return idLookup;
        }
        return rebuild(protocolVersion);
    }

    private Map<Class<? extends DefinedPacket>, Integer> classLookup(int protocolVersion) {
        if (protocolVersion == cachedVersion) {
            return classLookup;
        }
        rebuild(protocolVersion);
        return classLookup;
    }

    private synchronized Map<Integer, Entry> rebuild(int protocolVersion) {
        if (protocolVersion == cachedVersion) {
            return idLookup;
        }
        Map<Integer, Entry> byId = new HashMap<>();
        Map<Class<? extends DefinedPacket>, Integer> byClass = new HashMap<>();
        for (Entry entry : entries) {
            if (inRange(entry, protocolVersion)) {
                byId.putIfAbsent(entry.packetId(), entry);
                byClass.putIfAbsent(entry.packetClass(), entry.packetId());
            }
        }
        idLookup = byId;
        classLookup = byClass;
        cachedVersion = protocolVersion;
        return byId;
    }
}
