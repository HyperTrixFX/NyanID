package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Encryption request (0x01, login state, server to client).
 */
@Setter
@Getter
public class EncryptionRequest extends DefinedPacket {

    private String serverId;
    private byte[] publicKey;
    private byte[] verifyToken;

    public EncryptionRequest() {
    }

    public EncryptionRequest(String serverId, byte[] publicKey, byte[] verifyToken) {
        this.serverId = serverId;
        this.publicKey = publicKey;
        this.verifyToken = verifyToken;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.serverId = readString(buf);
        this.publicKey = readArray(buf);
        this.verifyToken = readArray(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.serverId, buf);
        writeArray(this.publicKey, buf);
        writeArray(this.verifyToken, buf);
    }

}
