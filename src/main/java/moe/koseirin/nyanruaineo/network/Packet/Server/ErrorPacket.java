package moe.koseirin.nyanruaineo.network.Packet.Server;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Interface.Packet;

@Setter
public class ErrorPacket implements Packet {
    private final boolean status;

    public ErrorPacket(boolean status) {
        this.status = status;
    }

    @Override
    public int packetId() {
        return 0x88;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeBoolean(status);
    }

    @Override
    public void decode(ByteBuf buf) {
    }
}
