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

import java.util.ArrayList;
import java.util.List;

/**
 * 这个包是发往客户端的 JoinGame，在不同版本下结构不一样。
 * 1.16 到 1.16.5（协议 735~763）包含了完整的维度注册表信息（存成 NBT 字节数组）；
 * 1.16 之前的版本用的是旧格式。这些解析都是为了能让代理端正常处理服务器切换流程。
 * 到了 1.20.2（764+），维度注册表的 NBT 没有了，只留个名字；764~765 用字符串表示维度，
 * 766+ 改用 VarInt 的维度 ID；1.20.5 往后又多了 secureProfile 字段。
 * 所有这些变化都解析了，目的是确保代理端在转发这个包的时候，字节能和收到时完全一样。
 */
@Data
@NoArgsConstructor
public class JoinGame extends DefinedPacket {

    private int entityId;
    private boolean hardcore;
    private short gameMode;
    private short previousGameMode;
    private List<String> worldNames = new ArrayList<>();
    /** Raw NBT bytes of the dimension registry (1.16-1.20.1 only). */
    private byte[] dimensions;
    /** Integer (pre-1.16), raw NBT bytes (1.16.2-1.18.2), the dimension id String (1.16-1.16.1 / 1.19-1.20.4) or a VarInt id (1.20.5+). */
    private Object dimension;
    private String worldName;
    private long seed;
    private short difficulty;
    private int maxPlayers;
    private String levelType;
    private int viewDistance;
    private int simulationDistance;
    private boolean reducedDebugInfo;
    private boolean normalRespawn;
    private boolean limitedCrafting;
    private boolean debug;
    private boolean flat;
    private String deathDimension;
    private long deathPosition;
    private int portalCooldown;
    private int seaLevel;
    private boolean onlineMode;
    private boolean secureProfile;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.entityId = buf.readInt();
        if (protocolVersion >= 751) {
            this.hardcore = buf.readBoolean();
        }
        if (protocolVersion < 764) {
            this.gameMode = buf.readUnsignedByte();
        }
        if (protocolVersion >= 735) {
            if (protocolVersion < 764) {
                this.previousGameMode = buf.readUnsignedByte();
            }
            int worldCount = readVarInt(buf);
            this.worldNames = new ArrayList<>(worldCount);
            for (int i = 0; i < worldCount; i++) {
                worldNames.add(readString(buf));
            }
            if (protocolVersion < 764) {
                this.dimensions = NbtUtils.readTagRaw(buf);
            }
        }

        if (protocolVersion >= 735) {
            if (protocolVersion >= 751 && protocolVersion <= 758) {
                this.dimension = NbtUtils.readTagRaw(buf, false);
            } else if (protocolVersion < 764) {
                this.dimension = readString(buf);
            }
            if (protocolVersion < 764) {
                this.worldName = readString(buf);
            }
        } else if (protocolVersion > 107) {
            this.dimension = buf.readInt();
        } else {
            this.dimension = (int) buf.readByte();
        }

        if (protocolVersion >= 573 && protocolVersion < 764) {
            this.seed = buf.readLong();
        }
        if (protocolVersion < 477) {                                   // pre-1.14
            this.difficulty = buf.readUnsignedByte();
        }
        if (protocolVersion >= 751) {
            this.maxPlayers = readVarInt(buf);
        } else {
            this.maxPlayers = buf.readUnsignedByte();
        }
        if (protocolVersion < 735) {
            this.levelType = readString(buf);
        }
        if (protocolVersion >= 477) {
            this.viewDistance = readVarInt(buf);
        }
        if (protocolVersion >= 757) {
            this.simulationDistance = readVarInt(buf);
        }
        if (protocolVersion >= 29) {
            this.reducedDebugInfo = buf.readBoolean();
        }
        if (protocolVersion >= 573) {
            this.normalRespawn = buf.readBoolean();
        }
        if (protocolVersion >= 764) {                                  // 1.20.2+ tail
            this.limitedCrafting = buf.readBoolean();
            if (protocolVersion >= 766) {
                this.dimension = readVarInt(buf);
            } else {
                this.dimension = readString(buf);
            }
            this.worldName = readString(buf);
            this.seed = buf.readLong();
            this.gameMode = buf.readUnsignedByte();
            this.previousGameMode = buf.readByte();
        }
        if (protocolVersion >= 735) {
            this.debug = buf.readBoolean();
            this.flat = buf.readBoolean();
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
        if (protocolVersion >= 776) {
            this.onlineMode = buf.readBoolean();
        }
        if (protocolVersion >= 766) {
            this.secureProfile = buf.readBoolean();
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(entityId);
        if (protocolVersion >= 751) {
            buf.writeBoolean(hardcore);
        }
        if (protocolVersion < 764) {
            buf.writeByte(gameMode);
        }
        if (protocolVersion >= 735) {
            if (protocolVersion < 764) {
                buf.writeByte(previousGameMode);
            }
            writeVarInt(worldNames.size(), buf);
            for (String world : worldNames) {
                writeString(world, buf);
            }
            if (protocolVersion < 764) {
                buf.writeBytes(dimensions == null ? new byte[0] : dimensions);
            }
        }

        if (protocolVersion >= 735) {
            if (protocolVersion >= 751 && protocolVersion <= 758) {
                buf.writeBytes((byte[]) dimension);
            } else if (protocolVersion < 764) {
                writeString((String) dimension, buf);
            }
            if (protocolVersion < 764) {
                writeString(worldName, buf);
            }
        } else if (protocolVersion > 107) {
            buf.writeInt((Integer) dimension);
        } else {
            buf.writeByte((Integer) dimension);
        }

        if (protocolVersion >= 573 && protocolVersion < 764) {
            buf.writeLong(seed);
        }
        if (protocolVersion < 477) {                                   // pre-1.14
            buf.writeByte(difficulty);
        }
        if (protocolVersion >= 751) {
            writeVarInt(maxPlayers, buf);
        } else {
            buf.writeByte(maxPlayers);
        }
        if (protocolVersion < 735) {
            writeString(levelType, buf);
        }
        if (protocolVersion >= 477) {
            writeVarInt(viewDistance, buf);
        }
        if (protocolVersion >= 757) {
            writeVarInt(simulationDistance, buf);
        }
        if (protocolVersion >= 29) {
            buf.writeBoolean(reducedDebugInfo);
        }
        if (protocolVersion >= 573) {
            buf.writeBoolean(normalRespawn);
        }
        if (protocolVersion >= 764) {                                  // 1.20.2+ tail
            buf.writeBoolean(limitedCrafting);
            if (protocolVersion >= 766) {
                writeVarInt((Integer) dimension, buf);
            } else {
                writeString((String) dimension, buf);
            }
            writeString(worldName, buf);
            buf.writeLong(seed);
            buf.writeByte(gameMode);
            buf.writeByte(previousGameMode);
        }
        if (protocolVersion >= 735) {
            buf.writeBoolean(debug);
            buf.writeBoolean(flat);
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
        if (protocolVersion >= 776) {
            buf.writeBoolean(onlineMode);
        }
        if (protocolVersion >= 766) {
            buf.writeBoolean(secureProfile);
        }
    }

    /**
     * 根据当前 JoinGame 包里的世界信息，构造一个对应的 Respawn 包发给客户端，
     * 另外，如果是 1.16 之前的版本，在切换服务器时中间会多一步“同维度重登录”，
     * 这个方法允许你在这时候可选地覆盖一下维度参数。
     */
    public Respawn toRespawn(Object dimensionOverride) {
        Respawn respawn = new Respawn();
        respawn.setDimension(dimensionOverride != null ? dimensionOverride : dimension);
        respawn.setWorldName(worldName);
        respawn.setSeed(seed);
        respawn.setDifficulty(difficulty);
        respawn.setGameMode(gameMode);
        respawn.setPreviousGameMode(previousGameMode);
        respawn.setLevelType(levelType);
        respawn.setDebug(debug);
        respawn.setFlat(flat);
        respawn.setCopyMeta((byte) 0);
        respawn.setDeathDimension(deathDimension);
        respawn.setDeathPosition(deathPosition);
        respawn.setPortalCooldown(portalCooldown);
        respawn.setSeaLevel(seaLevel);
        return respawn;
    }

    public Respawn toRespawn() {
        return toRespawn(null);
    }
}
