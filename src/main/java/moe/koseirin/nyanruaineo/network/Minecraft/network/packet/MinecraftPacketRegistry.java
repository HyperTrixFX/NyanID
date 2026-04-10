package moe.koseirin.nyanruaineo.network.Minecraft.network.packet;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class MinecraftPacketRegistry {
    private final Map<ConnectionState, Map<Integer, Supplier<? extends Packet>>> registry = new EnumMap<>(ConnectionState.class);

    public void register(ConnectionState state, int packetId, Supplier<? extends Packet> supplier) {
        registry.computeIfAbsent(state, k -> new HashMap<>()).put(packetId, supplier);
    }

    public Packet createPacket(ConnectionState state, int packetId) {
        Map<Integer, Supplier<? extends Packet>> map = registry.get(state);
        if (map == null) throw new IllegalArgumentException("No packets registered for state: " + state);
        Supplier<? extends Packet> supplier = map.get(packetId);
        if (supplier == null) throw new IllegalArgumentException("Unknown packet id: " + packetId + " for state " + state);
        return supplier.get();
    }
}
