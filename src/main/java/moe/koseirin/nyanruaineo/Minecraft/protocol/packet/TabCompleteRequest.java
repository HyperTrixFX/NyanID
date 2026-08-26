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
 * Serverbound的命令补全建议请求（1.13+ 的 {@code ServerboundCommandSuggestionPacket}）。
 * 当客户端在某个节点上执行自动补全（该节点标记为“向服务器请求建议”）时发送此包，
 * 其中携带当前光标位置的输入内容（例如 {@code "/server lob"}）。
 */
public class TabCompleteRequest extends DefinedPacket {

    @Getter
    @Setter
    private int transactionId;
    @Getter
    @Setter
    private String cursor;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.transactionId = readVarInt(buf);
        this.cursor = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(this.transactionId, buf);
        writeString(this.cursor, buf);
    }
}
