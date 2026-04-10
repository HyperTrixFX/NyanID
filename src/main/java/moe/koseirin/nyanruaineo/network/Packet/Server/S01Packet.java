package moe.koseirin.nyanruaineo.network.Packet.Server;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;

public class S01Packet implements Packet {
    private String uuid;
    private String nuid;

    public S01Packet() {}

    public S01Packet(String uuid, String nuid) {
        this.uuid = uuid;
        this.nuid = nuid;
    }

    @Override
    public int packetId() {
        return 0x84;
    }

    @Override
    public void encode(ByteBuf buf) {
        PacketCodecUtil.writeString(buf, uuid);
        PacketCodecUtil.writeString(buf, nuid);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.uuid = PacketCodecUtil.readString(buf);
        this.nuid = PacketCodecUtil.readString(buf);
    }

    // getters
    public String getUuid() { return uuid; }
    public String getNuid() { return nuid; }
}