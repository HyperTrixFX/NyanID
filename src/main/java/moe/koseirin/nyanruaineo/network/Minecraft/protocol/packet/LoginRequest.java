package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.ProtocolConstants;

import java.util.UUID;

/**
 * Login start packet (0x00, login state). The wire format changed across versions:
 * <ul>
 *   <li>1.19 &ndash; 1.19.2 (759 &ndash; 760): a secure-profile public key follows the name;</li>
 *   <li>1.19.1+ (760+): an optional UUID follows (mandatory from 1.20.2 / 764).</li>
 * </ul>
 */
public class LoginRequest extends DefinedPacket {

    @Setter
    @Getter
    private String username;
    private boolean hasPublicKey;
    private long publicKeyExpiresAt;
    private byte[] publicKeyBytes;
    private byte[] publicKeySignature;
    @Setter
    @Getter
    private UUID uuid;

    public LoginRequest() {
    }

    public LoginRequest(String username) {
        this.username = username;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.username = readString(buf);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19 && protocolVersion < ProtocolConstants.MINECRAFT_1_19_3) {
            this.hasPublicKey = buf.readBoolean();
            if (this.hasPublicKey) {
                this.publicKeyExpiresAt = buf.readLong();
                this.publicKeyBytes = readArray(buf);
                this.publicKeySignature = readArray(buf);
            }
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19_1) {
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_2 || buf.readBoolean()) {
                this.uuid = readUUID(buf);
            }
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.username, buf);
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19 && protocolVersion < ProtocolConstants.MINECRAFT_1_19_3) {
            buf.writeBoolean(this.hasPublicKey);
            if (this.hasPublicKey) {
                buf.writeLong(this.publicKeyExpiresAt);
                writeArray(this.publicKeyBytes, buf);
                writeArray(this.publicKeySignature, buf);
            }
        }
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_19_1) {
            if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_2) {
                writeUUID(this.uuid == null ? new UUID(0, 0) : this.uuid, buf);
            } else {
                if (this.uuid != null) {
                    buf.writeBoolean(true);
                    writeUUID(this.uuid, buf);
                } else {
                    buf.writeBoolean(false);
                }
            }
        }
    }

}
