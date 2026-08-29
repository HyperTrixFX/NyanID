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
 * Serverbound的签名聊天消息（1.19+）。
 * 仅解码开头的消息字符串用于命令拦截；
 * 剩余的签名有效载荷作为不透明尾部保留，
 * 以便转发时数据包能逐字节往返一致。
 */
public class ClientChat extends DefinedPacket {

    @Setter
    @Getter
    private String message;
    private byte[] tail = new byte[0];

    public ClientChat() {
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.message = readString(buf);
        this.tail = new byte[buf.readableBytes()];
        buf.readBytes(this.tail);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.message, buf);
        buf.writeBytes(this.tail);
    }

}
