package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.ProtocolConstants;

import java.util.UUID;

/**
 * 登录开始数据包（0x00，登录状态）。线路格式在不同版本间有所变化：
 * <ul>
 *   <li>1.19 – 1.19.2（759 – 760）：在名称后面跟随安全档案公钥；</li>
 *   <li>1.19.1+（760+）：增加一个可选的 UUID（从 1.20.2 / 764 开始为必选）。</li>
 *   <li>1.20.2+  ≥764 : UUID 变为必选字段，客户端必须提供自己的 UUID（与 Mojang 档案一致）。</li>
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
