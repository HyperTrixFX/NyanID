package moe.koseirin.nyanruaineo.network.Packet.Client;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;

public class BindAccountPacket implements Packet {
    private String code;
    private String uuid;

    public BindAccountPacket() {}

    public BindAccountPacket(String code, String uuid) {
        this.code = code;
        this.uuid = uuid;
    }

    @Override
    public int packetId() {
        return 0x03;
    }

    @Override
    public void encode(ByteBuf buf) {
        PacketCodecUtil.writeString(buf, code);
        PacketCodecUtil.writeString(buf, uuid);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.code = PacketCodecUtil.readString(buf);
        this.uuid = PacketCodecUtil.readString(buf);
    }

    public String getCode() { return code; }
    public String getUuid() { return uuid; }
}