package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 这个包是状态请求，ID 是 0x00，发生在状态（STATUS）阶段，由客户端发给服务端。
 * 它本身不包含任何数据，就是用来触发服务端回复状态响应的。
 */
public class StatusRequest extends DefinedPacket {

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
    }
}
