package moe.koseirin.nyanruaineo.network.Packet.Client;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;

@Getter
public class UpdateOnlinePacket implements Packet {
    private String servername;
    private int online;

    public UpdateOnlinePacket() {}

    public UpdateOnlinePacket(String servername, int online) {
        this.servername = servername;
        this.online = online;
    }

    @Override
    public int packetId() {
        return 0x02;
    }

    @Override
    public void encode(ByteBuf buf) {
        PacketCodecUtil.writeString(buf, servername);
        PacketCodecUtil.writeVarInt(buf, online);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.servername = PacketCodecUtil.readString(buf);
        this.online = PacketCodecUtil.readVarInt(buf);
    }

}