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
public class EncryptionResponsePacket implements Packet {
    private byte[] sharedSecret;
    private byte[] verifyToken;

    @Override
    public int packetId() {
        return 0x01; // Login 阶段 Encryption Response 的包 ID
    }

    @Override
    public void encode(ByteBuf buf) {
        VarIntCodec.writeVarInt(buf, sharedSecret.length);
        buf.writeBytes(sharedSecret);
        VarIntCodec.writeVarInt(buf, verifyToken.length);
        buf.writeBytes(verifyToken);
    }

    @Override
    public void decode(ByteBuf buf) {
        int secretLen = VarIntCodec.readVarInt(buf);
        this.sharedSecret = new byte[secretLen];
        buf.readBytes(this.sharedSecret);
        int tokenLen = VarIntCodec.readVarInt(buf);
        this.verifyToken = new byte[tokenLen];
        buf.readBytes(this.verifyToken);
    }
}
