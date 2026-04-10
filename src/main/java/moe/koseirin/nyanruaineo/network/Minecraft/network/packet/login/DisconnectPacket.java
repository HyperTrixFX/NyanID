package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

public class DisconnectPacket  implements Packet {
    private String reason;

    public DisconnectPacket() {}

    public DisconnectPacket(String reason) {
        this.reason = reason;
    }

    @Override
    public int packetId() {
        return 0x00;
    }

    @Override
    public void encode(ByteBuf buf) {
        VarIntCodec.writeString(buf, reason);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.reason = VarIntCodec.readString(buf);
    }
}
