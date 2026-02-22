package moe.koseirin.nyanruaineo.network.Packet.Client;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;

public class CheckBindPacket implements Packet {
    private String uuid;

    public CheckBindPacket() {}

    public CheckBindPacket(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public int packetId() {
        return 0x04;
    }

    @Override
    public void encode(ByteBuf buf) {
        PacketCodecUtil.writeString(buf, uuid);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.uuid = PacketCodecUtil.readString(buf);
    }

    public String getUuid() { return uuid; }
}