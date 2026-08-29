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
 * 加密请求 / 客户端bound "hello"（0x01，登录状态，服务端发往客户端）。
 * 自 1.20.5（协议 766）起，在验证令牌之后增加了一个尾随的 {@code shouldAuthenticate} 布尔值
 * —— 如果没有它，1.20.5+ 客户端将无法解码该数据包（报错 "Failed to decode packet 'clientbound/minecraft:hello'"）
 * 并断开连接。
 */
@Setter
@Getter
public class EncryptionRequest extends DefinedPacket {

    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;
    /** 1.20.5+: whether the server will authenticate the client (the proxy always does). */
    private boolean shouldAuthenticate = true;

    public EncryptionRequest() {
    }

    public EncryptionRequest(String serverId, byte[] publicKey, byte[] verifyToken) {
        this.serverId = serverId;
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.serverId = readString(buf);
        this.publicKey = readArray(buf);
        this.verifyToken = readArray(buf);
        if (protocolVersion >= 766) {                                  // 1.20.5+
            this.shouldAuthenticate = buf.readBoolean();
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.serverId, buf);
        writeArray(this.publicKey, buf);
        writeArray(this.verifyToken, buf);
        if (protocolVersion >= 766) {                                  // 1.20.5+
            buf.writeBoolean(this.shouldAuthenticate);
        }
    }

}
