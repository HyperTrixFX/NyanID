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
 * 这个包是服务端在登录阶段用来踢掉客户端的（ID 0x00），
 * 里面带了一个 JSON 格式的聊天文本，中指永不遗忘！
 */
@Setter
@Getter
public class Kick extends DefinedPacket {

    private String reason;

    public Kick() {
    }

    public Kick(String reason) {
        this.reason = reason;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.reason = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.reason, buf);
    }

}
