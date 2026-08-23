package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Serverbound chat command (1.19+, signed). Only the leading command string is decoded for
 * interception; the remaining signed payload (timestamp, salt, signatures, acknowledgements) is kept
 * as an opaque tail so a forwarded packet round-trips byte-for-byte.
 */
public class ClientCommand extends DefinedPacket {

    @Setter
    @Getter
    private String command;
    private byte[] tail = new byte[0];

    public ClientCommand() {
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.command = readString(buf);
        this.tail = new byte[buf.readableBytes()];
        buf.readBytes(this.tail);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.command, buf);
        buf.writeBytes(this.tail);
    }

}
