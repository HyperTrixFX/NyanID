package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/** Clientbound view distance packet (mirrors BungeeCord's {@code ViewDistance}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewDistance extends DefinedPacket {

    private int distance;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.distance = readVarInt(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(distance, buf);
    }
}
