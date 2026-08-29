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
 * 不带签名发送的服务器bound聊天命令（1.20.5+ 中 {@code ServerboundChatCommandPacket} 的变体，
 * 当该命令不可签名时使用 —— 例如它不在客户端的命令树中）。
 */
public class UnsignedClientCommand extends DefinedPacket {

    @Getter
    @Setter
    private String command;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.command = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.command, buf);
    }
}
