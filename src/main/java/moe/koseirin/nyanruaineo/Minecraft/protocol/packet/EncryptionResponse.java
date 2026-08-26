package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 这是客户端在登录阶段发给服务端的加密响应包（ID 0x01）。
 * 内容通常是共享密钥和验证令牌。
 * 不过 1.19 到 1.19.2 版本有点特殊，客户端可能不发验证令牌，改发盐值和消息签名，
 * 这是为了做安全档案证明（secure profile proof）。
 */
@Setter
@Getter
public class EncryptionResponse extends DefinedPacket {

    private byte[] sharedSecret;
    private byte[] verifyToken;
    private boolean hasSaltSignature;
    private long salt;
    private byte[] messageSignature;

    public EncryptionResponse() {
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.sharedSecret = readArray(buf);
        if (protocolVersion < 759 || protocolVersion >= 761 || buf.readBoolean()) {
            this.verifyToken = readArray(buf);
        } else {
            // 1.19-1.19.2 secure profile: salt + signature instead of the verify token.
            this.hasSaltSignature = true;
            this.salt = buf.readLong();
            this.messageSignature = readArray(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeArray(this.sharedSecret, buf);
        if (verifyToken != null) {
            if (protocolVersion >= 759 && protocolVersion < 761) {
                buf.writeBoolean(true);
            }
            writeArray(this.verifyToken, buf);
        } else {
            if (protocolVersion >= 759 && protocolVersion < 761) {
                buf.writeBoolean(false);
            }
            buf.writeLong(salt);
            writeArray(this.messageSignature, buf);
        }
    }

}
