package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

public class StatusRequestPacket implements Packet {
    @Override
    public int packetId() { return 0x00; }

    @Override
    public void encode(ByteBuf buf) {}

    @Override
    public void decode(ByteBuf buf) {}
}
