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
public class EncryptionRequestPacket implements Packet {
    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;

    public EncryptionRequestPacket(String serverId, byte[] publicKey, byte[] verifyToken) {
        this.serverId = serverId;
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
    }

    @Override
    public int packetId() {
        return 0x01; // Login 阶段 Encryption Request 的包 ID
    }

    @Override
    public void encode(ByteBuf buf) {
        VarIntCodec.writeString(buf, serverId);
        VarIntCodec.writeVarInt(buf, publicKey.length);
        buf.writeBytes(publicKey);
        VarIntCodec.writeVarInt(buf, verifyToken.length);
        buf.writeBytes(verifyToken);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.serverId = VarIntCodec.readString(buf);
        int keyLen = VarIntCodec.readVarInt(buf);
        this.publicKey = new byte[keyLen];
        buf.readBytes(this.publicKey);
        int tokenLen = VarIntCodec.readVarInt(buf);
        this.verifyToken = new byte[tokenLen];
        buf.readBytes(this.verifyToken);
    }
}
