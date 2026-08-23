package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Encryption response (0x01, login state, client to server).
 */
@Setter
@Getter
public class EncryptionResponse extends DefinedPacket {

    private byte[] sharedSecret;
    private byte[] verifyToken;

    public EncryptionResponse() {
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.sharedSecret = readArray(buf);
        this.verifyToken = readArray(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeArray(this.sharedSecret, buf);
        writeArray(this.verifyToken, buf);
    }

}
