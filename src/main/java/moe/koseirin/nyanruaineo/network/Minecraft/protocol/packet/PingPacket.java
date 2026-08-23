package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Ping request/response (0x01, status state, both directions). Carries a single long payload.
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
