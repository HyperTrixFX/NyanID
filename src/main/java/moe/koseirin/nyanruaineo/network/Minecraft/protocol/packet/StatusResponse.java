package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Status response (0x00, status state, server to client). The payload is the server list ping JSON.
 */
@Setter
@Getter
public class StatusResponse extends DefinedPacket {

    private String json;

    public StatusResponse() {
    }

    public StatusResponse(String json) {
        this.json = json;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.json = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(this.json, buf);
    }

}
