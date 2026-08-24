package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.UUID;

/** Clientbound boss bar packet, 1.9+ (mirrors BungeeCord's {@code BossBar}). */
@Data
@NoArgsConstructor
public class BossBar extends DefinedPacket {

    private UUID uuid;
    private int action;
    private String title;
    private float health;
    private int color;
    private int division;
    private byte flags;

    public BossBar(UUID uuid, int action) {
        this.uuid = uuid;
        this.action = action;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.uuid = readUUID(buf);
        this.action = readVarInt(buf);
        switch (action) {
            case 0:
                this.title = readString(buf);
                this.health = buf.readFloat();
                this.color = readVarInt(buf);
                this.division = readVarInt(buf);
                this.flags = buf.readByte();
                break;
            case 2:
                this.health = buf.readFloat();
                break;
            case 3:
                this.title = readString(buf);
                break;
            case 4:
                this.color = readVarInt(buf);
                this.division = readVarInt(buf);
                break;
            case 5:
                this.flags = buf.readByte();
                break;
            default:
                break;
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeUUID(uuid, buf);
        writeVarInt(action, buf);
        switch (action) {
            case 0:
                writeString(title, buf);
                buf.writeFloat(health);
                writeVarInt(color, buf);
                writeVarInt(division, buf);
                buf.writeByte(flags);
                break;
            case 2:
                buf.writeFloat(health);
                break;
            case 3:
                writeString(title, buf);
                break;
            case 4:
                writeVarInt(color, buf);
                writeVarInt(division, buf);
                break;
            case 5:
                buf.writeByte(flags);
                break;
            default:
                break;
        }
    }
}
