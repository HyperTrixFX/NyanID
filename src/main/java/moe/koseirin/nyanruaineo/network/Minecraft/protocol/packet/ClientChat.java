package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Serverbound signed chat message (1.19+). Only the leading message string is decoded for command
 * interception; the remaining signed payload is kept as an opaque tail so a forwarded packet
 * round-trips byte-for-byte.
 */
public class ClientChat extends DefinedPacket {

    @Setter
    @Getter
    private String message;
    private byte[] tail = new byte[0];

    public ClientChat() {
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.message = readString(buf);
        this.tail = new byte[buf.readableBytes()];
        buf.readBytes(this.tail);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.message, buf);
        buf.writeBytes(this.tail);
    }

}
