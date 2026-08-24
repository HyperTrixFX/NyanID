package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.util.NbtUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Clientbound JoinGame packet, parsed per version following BungeeCord's {@code Login}: full
 * fields for 1.16+ (735-763, with the dimension registry carried as raw NBT bytes) and the legacy
 * fields below 1.16, so the server connector can drive the complete BungeeCord switch flow.
 */
@Data
@NoArgsConstructor
public class JoinGame extends DefinedPacket {

    private int entityId;
    private boolean hardcore;
    private short gameMode;
    private short previousGameMode;
    private List<String> worldNames = new ArrayList<>();
    /** Raw NBT bytes of the dimension registry (1.16+). */
    private byte[] dimensions;
    /** Integer (pre-1.16), raw NBT bytes (1.16.2-1.18.2) or the dimension id String (1.16-1.16.1 / 1.19-1.20.1). */
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
    private boolean debug;
    private boolean flat;
    private String deathDimension;
    private long deathPosition;
    private int portalCooldown;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.entityId = buf.readInt();
        if (protocolVersion >= 751) {
            this.hardcore = buf.readBoolean();
        }
        this.gameMode = buf.readUnsignedByte();

        if (protocolVersion >= 735) {
            this.previousGameMode = buf.readUnsignedByte();

            int worldCount = readVarInt(buf);
            this.worldNames = new ArrayList<>(worldCount);
            for (int i = 0; i < worldCount; i++) {
                worldNames.add(readString(buf));
            }

            this.dimensions = NbtUtils.readTagRaw(buf);
            if (protocolVersion >= 751 && protocolVersion <= 758) {
                this.dimension = NbtUtils.readTagRaw(buf, false);      // 1.16.2-1.18.2 dimension type
            } else {
                this.dimension = readString(buf);                      // 1.16-1.16.1 / 1.19-1.20.1
            }
            this.worldName = readString(buf);
        } else if (protocolVersion > 107) {
            this.dimension = buf.readInt();
        } else {
            this.dimension = (int) buf.readByte();
        }

        if (protocolVersion >= 573) {
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
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeInt(entityId);
        if (protocolVersion >= 751) {
            buf.writeBoolean(hardcore);
        }
        buf.writeByte(gameMode);

        if (protocolVersion >= 735) {
            buf.writeByte(previousGameMode);

            writeVarInt(worldNames.size(), buf);
            for (String world : worldNames) {
                writeString(world, buf);
            }

            buf.writeBytes(dimensions == null ? new byte[0] : dimensions);
            if (protocolVersion >= 751 && protocolVersion <= 758) {
                buf.writeBytes((byte[]) dimension);
            } else {
                writeString((String) dimension, buf);
            }
            writeString(worldName, buf);
        } else if (protocolVersion > 107) {
            buf.writeInt((Integer) dimension);
        } else {
            buf.writeByte((Integer) dimension);
        }

        if (protocolVersion >= 573) {
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
    }

    /**
     * Builds the Respawn packet carrying this JoinGame's world data (BungeeCord behaviour), with
     * an optional dimension override for the pre-1.16 same-dimension intermediate respawn.
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
        return respawn;
    }

    public Respawn toRespawn() {
        return toRespawn(null);
    }
}
