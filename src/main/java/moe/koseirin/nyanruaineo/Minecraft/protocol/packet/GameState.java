package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/** 客户端bound游戏状态变更数据包 */
@EqualsAndHashCode(callSuper = true)
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
