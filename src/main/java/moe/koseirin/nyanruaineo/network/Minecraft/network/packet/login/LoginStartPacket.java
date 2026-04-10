package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

@Data
@NoArgsConstructor
public class LoginStartPacket implements Packet {
    private String username;

    public LoginStartPacket(String username) { this.username = username; }

    @Override public int packetId() { return 0x00; }

    @Override
    public void encode(ByteBuf buf) {
        VarIntCodec.writeString(buf, username);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.username = VarIntCodec.readString(buf);
    }

}
