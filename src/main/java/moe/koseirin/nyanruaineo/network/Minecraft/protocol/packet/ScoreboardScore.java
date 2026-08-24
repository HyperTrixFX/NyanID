package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/** Clientbound scoreboard score packet (mirrors BungeeCord's {@code ScoreboardScore}, < 1.20.3). */
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
