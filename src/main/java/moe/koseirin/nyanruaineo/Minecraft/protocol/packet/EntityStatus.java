package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/** Clientbound 实体状态数据包 */
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
