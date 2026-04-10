package moe.koseirin.nyanruaineo.network.Interface;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;

public interface Packet {
    // pid
    int packetId();
    // 将包数据写入 ByteBuf
    void encode(ByteBuf buf);
    void decode(ByteBuf buf);

}

