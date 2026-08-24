package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/** Clientbound entity status packet (mirrors BungeeCord's {@code EntityStatus}). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityStatus extends DefinedPacket {

    public static final byte DEBUG_INFO_REDUCED = 22;
    public static final byte DEBUG_INFO_NORMAL = 23;

    private int entityId;
    private byte status;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.entityId = buf.readInt();
        this.status = buf.readByte();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(entityId);
        buf.writeByte(status);
    }
}
