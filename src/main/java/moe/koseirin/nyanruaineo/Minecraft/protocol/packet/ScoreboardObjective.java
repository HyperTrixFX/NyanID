package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

import java.util.Locale;

/**
 * 客户端方向的计分板目标数据包（适用于 1.20.3 以下版本）。
 */
@Data
@NoArgsConstructor
public class ScoreboardObjective extends DefinedPacket {

    private String name;
    private byte action;
    private String value;
    private String typeName;
    private int typeOrdinal;

    public ScoreboardObjective(String name, String value, String typeName, int typeOrdinal, byte action) {
        this.name = name;
        this.value = value;
        this.typeName = typeName;
        this.typeOrdinal = typeOrdinal;
        this.action = action;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.name = readString(buf);
        this.action = buf.readByte();
        if (action == 0 || action == 2) {
            this.value = readString(buf);
            if (protocolVersion >= 393) {                              // 1.13+
                this.typeOrdinal = readVarInt(buf);
            } else {
                this.typeName = readString(buf);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(name, buf);
        buf.writeByte(action);
        if (action == 0 || action == 2) {
            writeString(value, buf);
            if (protocolVersion >= 393) {                              // 1.13+
                writeVarInt(typeOrdinal, buf);
            } else {
                writeString(typeName, buf);
            }
        }
    }

    public static String typeToString(int ordinal) {
        return ordinal == 0 ? "integer" : "hearts";
    }

    public static int typeFromString(String type) {
        return "hearts".equals(type.toLowerCase(Locale.ROOT)) ? 1 : 0;
    }
}
