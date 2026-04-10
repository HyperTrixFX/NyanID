package moe.koseirin.nyanruaineo.network.Packet.Client;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Interface.Packet;

public class HeartbeatResponsePacket implements Packet {
    public HeartbeatResponsePacket() {}

    @Override
    public int packetId() {
        return 0x81;
    }

    @Override
    public void encode(ByteBuf buf) {
    }

    @Override
    public void decode(ByteBuf buf) {
    }
}