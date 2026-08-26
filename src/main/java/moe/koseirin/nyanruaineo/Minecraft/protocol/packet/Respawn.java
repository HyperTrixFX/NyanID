package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.util.NbtUtils;

/**
 * 客户端方向的重生（Respawn）数据包（与 BungeeCord 的 {@code Respawn} 对应）。
 * 在 1.16 以下版本中，维度为 int 类型，同时携带难度（difficulty）和世界类型（levelType）；
 * 1.16+ 版本中，维度改为 String 类型（1.16.2-1.18.2 为原始 NBT），
 * 并增加种子（seed）/调试模式（debug）/超平坦（flat）/复制元数据（copy-meta）/死亡位置（death-location）/传送门冷却（portal-cooldown）等字段。
 * 1.20.2+ 版本中，copy-meta 字节移至数据包末尾；
 * 1.20.5+ 版本中，维度改为 VarInt ID；
 * 1.21.2+ 版本新增海平面（sea level）字段。
 */
@Data
@NoArgsConstructor
public class Respawn extends DefinedPacket {

    /** Integer (pre-1.16), raw NBT bytes (1.16.2-1.18.2), the dimension id String (1.16-1.16.1 / 1.19-1.20.4) or a VarInt id (1.20.5+). */
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
    private int seaLevel;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= 735) {
            if (protocolVersion >= 766) {
                this.dimension = readVarInt(buf);
            } else if (protocolVersion >= 751 && protocolVersion <= 758) {
                this.dimension = NbtUtils.readTagRaw(buf, false);
            } else {
                this.dimension = readString(buf);
            }
            this.worldName = readString(buf);
        } else {
            this.dimension = buf.readInt();
        }
        if (protocolVersion >= 573) {
            this.seed = buf.readLong();
        }
        if (protocolVersion < 477) {                                   // pre-1.14
            this.difficulty = buf.readUnsignedByte();
        }
        this.gameMode = buf.readUnsignedByte();
        if (protocolVersion >= 735) {
            this.previousGameMode = buf.readUnsignedByte();
            this.debug = buf.readBoolean();
            this.flat = buf.readBoolean();
            if (protocolVersion < 764) {
                this.copyMeta = buf.readByte();
            }
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
        if (protocolVersion >= 768) {
            this.seaLevel = readVarInt(buf);
        }
        if (protocolVersion >= 764) {                                  // 1.20.2+: copy-meta at the end
            this.copyMeta = buf.readByte();
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= 735) {
            if (protocolVersion >= 766) {
                writeVarInt((Integer) dimension, buf);
            } else if (protocolVersion >= 751 && protocolVersion <= 758) {
                buf.writeBytes((byte[]) dimension);
            } else {
                writeString((String) dimension, buf);
            }
            writeString(worldName, buf);
        } else {
            buf.writeInt((Integer) dimension);
        }
        if (protocolVersion >= 573) {
            buf.writeLong(seed);
        }
        if (protocolVersion < 477) {                                   // pre-1.14
            buf.writeByte(difficulty);
        }
        buf.writeByte(gameMode);
        if (protocolVersion >= 735) {
            buf.writeByte(previousGameMode);
            buf.writeBoolean(debug);
            buf.writeBoolean(flat);
            if (protocolVersion < 764) {
                buf.writeByte(copyMeta);
            }
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
        if (protocolVersion >= 768) {
            writeVarInt(seaLevel, buf);
        }
        if (protocolVersion >= 764) {                                  // 1.20.2+: copy-meta at the end
            buf.writeByte(copyMeta);
        }
    }
}
