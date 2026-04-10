package moe.koseirin.nyanruaineo.network.Packet.Server;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Interface.Packet;

public class HeartbeatPacket implements Packet {
    public HeartbeatPacket() {}

    @Override
    public int packetId() {
        return 0x01;
    }

    @Override
    public void encode(ByteBuf buf) {
    }

    @Override
    public void decode(ByteBuf buf) {
    }
}
