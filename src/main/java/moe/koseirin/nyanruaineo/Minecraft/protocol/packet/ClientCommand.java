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
 * 这是客户端发往服务端的签名版聊天命令包（1.19 以上才有）。
 * 我们只解析最前面的命令字符串，用来判断要不要拦截处理；
 * 剩下的签名相关数据（时间戳、salt、签名、确认回执等）原封不动地保留成一段“黑盒”尾巴，
 * 转发的时候把整段尾巴带上，确保每个字节都和原来一样，签名才不会失效。
 */
public class ClientCommand extends DefinedPacket {

    @Setter
    @Getter
    private String command;
    private byte[] tail = new byte[0];

    public ClientCommand() {
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.command = readString(buf);
        this.tail = new byte[buf.readableBytes()];
        buf.readBytes(this.tail);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.command, buf);
        buf.writeBytes(this.tail);
    }

}
