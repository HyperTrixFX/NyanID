package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Serverbound chat message (pre-1.19 format: a single string). Used to intercept player commands
 * before they reach the backend. 1.19+ uses the signed chat packet, which is relayed raw instead.
 */
@Setter
@Getter
public class Chat extends DefinedPacket {

    private String message;

    public Chat() {
    }

    public Chat(String message) {
        this.message = message;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.message = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.message, buf);
    }

}
