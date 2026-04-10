package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

@Data
@NoArgsConstructor
public class PingRequestPacket implements Packet {
    private long payload;

    public PingRequestPacket(long payload) { this.payload = payload; }

    @Override public int packetId() { return 0x01; }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeLong(payload);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.payload = buf.readLong();
    }

}
