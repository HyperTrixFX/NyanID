package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 客户端方向的计分板分数数据包（适用于 1.20.3 以下版本）。
 */
@Data
@NoArgsConstructor
public class ScoreboardScore extends DefinedPacket {

    private String itemName;
    private byte action;
    private String scoreName;
    private int value;

    public ScoreboardScore(String itemName, byte action, String scoreName, int value) {
        this.itemName = itemName;
        this.action = action;
        this.scoreName = scoreName;
        this.value = value;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.itemName = readString(buf);
        this.action = buf.readByte();
        this.scoreName = readString(buf);
        if (action != 1) {
            this.value = readVarInt(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(itemName, buf);
        buf.writeByte(action);
        writeString(scoreName, buf);
        if (action != 1) {
            writeVarInt(value, buf);
        }
    }
}
