package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.util.NbtUtils;

/**
 * Clientbound Respawn packet (mirrors BungeeCord's {@code Respawn}). Below 1.16 the dimension is
 * an int and difficulty/levelType are carried; 1.16+ carries the dimension as a String (or raw
 * NBT for 1.16.2-1.18.2) plus seed/debug/flat/copy-meta/death-location/portal-cooldown.
 */
@Data
@NoArgsConstructor
public class Respawn extends DefinedPacket {

    /** Integer (pre-1.16), raw NBT bytes (1.16.2-1.18.2) or the dimension id String (1.16-1.16.1 / 1.19-1.20.1). */
    private Object dimension;
    private String worldName;
    private long seed;
    private short difficulty;
    private short gameMode;
    private short previousGameMode;
    private String levelType;
    private boolean debug;
    private boolean flat;
    private byte copyMeta;
    private String deathDimension;
    private long deathPosition;
    private int portalCooldown;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= 735) {
            if (protocolVersion >= 751 && protocolVersion <= 758) {
                this.dimension = NbtUtils.readTagRaw(buf, false);
            } else {
                this.dimension = readString(buf);
            }
            this.worldName = readString(buf);
            this.seed = buf.readLong();
        } else {
            this.dimension = buf.readInt();
        }
        if (protocolVersion < 477) {                                   // pre-1.14
            this.difficulty = buf.readUnsignedByte();
        }
        this.gameMode = buf.readUnsignedByte();
        if (protocolVersion >= 735) {
            this.previousGameMode = buf.readUnsignedByte();
            this.debug = buf.readBoolean();
            this.flat = buf.readBoolean();
            this.copyMeta = buf.readByte();
        } else {
            this.levelType = readString(buf);
        }
        if (protocolVersion >= 759) {
            if (buf.readBoolean()) {
                this.deathDimension = readString(buf);
                this.deathPosition = buf.readLong();
            }
        }
        if (protocolVersion >= 763) {
            this.portalCooldown = readVarInt(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= 735) {
            if (protocolVersion >= 751 && protocolVersion <= 758) {
                buf.writeBytes((byte[]) dimension);
            } else {
                writeString((String) dimension, buf);
            }
            writeString(worldName, buf);
            buf.writeLong(seed);
        } else {
            buf.writeInt((Integer) dimension);
        }
        if (protocolVersion < 477) {                                   // pre-1.14
            buf.writeByte(difficulty);
        }
        buf.writeByte(gameMode);
        if (protocolVersion >= 735) {
            buf.writeByte(previousGameMode);
            buf.writeBoolean(debug);
            buf.writeBoolean(flat);
            buf.writeByte(copyMeta);
        } else {
            writeString(levelType, buf);
        }
        if (protocolVersion >= 759) {
            if (deathDimension != null) {
                buf.writeBoolean(true);
                writeString(deathDimension, buf);
                buf.writeLong(deathPosition);
            } else {
                buf.writeBoolean(false);
            }
        }
        if (protocolVersion >= 763) {
            writeVarInt(portalCooldown, buf);
        }
    }
}
