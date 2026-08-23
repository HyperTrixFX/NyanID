package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Disconnect / kick packet (0x00, login state, server to client). The payload is a JSON chat
 * component string.
 */
@Setter
@Getter
public class Kick extends DefinedPacket {

    private String reason;

    public Kick() {
    }

    public Kick(String reason) {
        this.reason = reason;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.reason = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.reason, buf);
    }

}
