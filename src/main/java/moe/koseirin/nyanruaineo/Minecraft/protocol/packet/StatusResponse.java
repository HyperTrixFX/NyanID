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
 * 这是状态响应包，ID 是 0x00，发生在状态（STATUS）阶段，由服务端发给客户端。
 * 包里面装的就是服务器列表 Ping 需要的那个 JSON 数据（包含 MOTD、在线人数、版本信息等）。
 */
@Setter
@Getter
public class StatusResponse extends DefinedPacket {

    private String json;

    public StatusResponse() {
    }

    public StatusResponse(String json) {
        this.json = json;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.json = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.json, buf);
    }

}
