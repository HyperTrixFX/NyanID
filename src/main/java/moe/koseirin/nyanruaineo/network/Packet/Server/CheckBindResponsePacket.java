package moe.koseirin.nyanruaineo.network.Packet.Server;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;

@Getter
public class CheckBindResponsePacket implements Packet {
    // getters
    private boolean bind;
    private String muid;
    private String uuid;
    private String username;

    public CheckBindResponsePacket() {}

    public CheckBindResponsePacket(boolean bind, String muid, String uuid, String username) {
        this.bind = bind;
        this.muid = muid;
        this.uuid = uuid;
        this.username = username;
    }

    @Override
    public int packetId() {
        return 0x83;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeBoolean(bind);
        if (bind) {
            PacketCodecUtil.writeString(buf, muid);
            PacketCodecUtil.writeString(buf, uuid);
            PacketCodecUtil.writeString(buf, username);
        }
    }

    @Override
    public void decode(ByteBuf buf) {
        this.bind = buf.readBoolean();
        if (bind) {
            this.muid = PacketCodecUtil.readString(buf);
            this.uuid = PacketCodecUtil.readString(buf);
            this.username = PacketCodecUtil.readString(buf);
        }
    }

}