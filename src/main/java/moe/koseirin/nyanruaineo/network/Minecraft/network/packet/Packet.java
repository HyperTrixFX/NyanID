package moe.koseirin.nyanruaineo.network.Minecraft.network.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;

public interface Packet {
    int packetId();
    void encode(ByteBuf buf);
    void decode(ByteBuf buf);
}
