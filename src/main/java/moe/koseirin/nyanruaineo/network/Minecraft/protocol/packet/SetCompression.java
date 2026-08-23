package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Set Compression (0x03, login state, server to client). Enables zlib packet compression with the
 * given threshold; packets at or above the threshold are compressed from the next packet onward.
 */
@Setter
@Getter
public class SetCompression extends DefinedPacket {

    private int threshold;

    public SetCompression() {
    }

    public SetCompression(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.threshold = readVarInt(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(this.threshold, buf);
    }

}
