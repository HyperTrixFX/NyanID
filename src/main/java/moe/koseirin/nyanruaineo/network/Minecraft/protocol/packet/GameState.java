package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/** Clientbound game state change packet (mirrors BungeeCord's {@code GameState}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameState extends DefinedPacket {

    public static final short IMMEDIATE_RESPAWN = 11;

    private short state;
    private float value;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.state = buf.readUnsignedByte();
        this.value = buf.readFloat();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeByte(state);
        buf.writeFloat(value);
    }
}
