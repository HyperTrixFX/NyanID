package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clientbound player info remove packet (1.19.3+), mirroring BungeeCord's
 * {@code PlayerListItemRemove}. Registered so the proxy can forget the removed players' TabList
 * names when it decorates display names.
 */
@Data
@NoArgsConstructor
public class PlayerInfoRemove extends DefinedPacket {

    private List<UUID> uuids = new ArrayList<>();

    public PlayerInfoRemove(List<UUID> uuids) {
        this.uuids = uuids;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        int count = readVarInt(buf);
        this.uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(readUUID(buf));
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(uuids.size(), buf);
        for (UUID uuid : uuids) {
            writeUUID(uuid, buf);
        }
    }
}
