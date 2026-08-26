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
 * Ping 请求/响应（0x01，状态阶段，双向）。携带一个单独的 long 类型负载。
 */
@Setter
@Getter
public class PingPacket extends DefinedPacket {

    private long payload;

    public PingPacket() {
    }

    public PingPacket(long payload) {
        this.payload = payload;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.payload = buf.readLong();
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeLong(this.payload);
    }

}
