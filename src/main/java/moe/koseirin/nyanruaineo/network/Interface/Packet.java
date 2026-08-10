package moe.koseirin.nyanruaineo.network.Interface;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;

public interface Packet {
    // pid
    int packetId();
    // ByteBuf
    void encode(ByteBuf buf);
    void decode(ByteBuf buf);

}

