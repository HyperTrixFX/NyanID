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
 * Serverbound聊天消息（1.19之前的格式：只有一个字符串）。用于在命令到达后端之前拦截玩家命令。
 * 1.19+ 使用签名聊天包，该包会被原样转发而不在此处理。
 */
@Setter
@Getter
public class Chat extends DefinedPacket {

    private String message;

    public Chat() {
    }

    public Chat(String message) {
        this.message = message;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.message = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.message, buf);
    }

}
