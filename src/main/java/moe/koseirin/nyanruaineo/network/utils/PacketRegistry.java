package moe.koseirin.nyanruaineo.network.utils;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;


@Slf4j
@Component
public class PacketRegistry {
    private final Map<Integer, Supplier<? extends Packet>> suppliers = new HashMap<>();

    public <T extends Packet> void register(int packetId, Supplier<T> supplier) {
        suppliers.put(packetId, supplier);
    }

    public Packet createPacket(int packetId) {
        Supplier<? extends Packet> supplier = suppliers.get(packetId);
        if (supplier == null) {
            log.warn("Unknown packet id: {}", packetId);
        }
        return Objects.requireNonNull(supplier).get();
    }
}