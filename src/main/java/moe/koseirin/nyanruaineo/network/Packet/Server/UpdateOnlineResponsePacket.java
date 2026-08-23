package moe.koseirin.nyanruaineo.network.Packet.Server;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import moe.koseirin.nyanruaineo.network.Interface.Packet;

@Getter
public class UpdateOnlineResponsePacket implements Packet {
    private boolean success;

    public UpdateOnlineResponsePacket() {}

    public UpdateOnlineResponsePacket(boolean success) {
        this.success = success;
    }

    @Override
    public int packetId() {
        return 0x82;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeBoolean(success);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.success = buf.readBoolean();
    }

}