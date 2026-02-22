package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status;

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
public class StatusResponsePacket implements Packet {
    private String jsonResponse;

    public StatusResponsePacket(String jsonResponse) { this.jsonResponse = jsonResponse; }

    @Override public int packetId() { return 0x00; }

    @Override
    public void encode(ByteBuf buf) {
        VarIntCodec.writeString(buf, jsonResponse);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.jsonResponse = VarIntCodec.readString(buf);
    }
}
