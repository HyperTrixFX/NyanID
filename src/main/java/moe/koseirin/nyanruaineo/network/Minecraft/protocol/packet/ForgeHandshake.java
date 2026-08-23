package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.nio.charset.StandardCharsets;

/**
 * The pre-1.8 Forge (FML 1.7) handshake packet (0x250), sent in the handshake/login phase before
 * the vanilla login start. Carries a channel ("FML|HS") plus the FML handshake payload. The proxy
 * terminates this handshake itself with an empty mod list.
 */
@Setter
@Getter
public class ForgeHandshake extends DefinedPacket {

    public static final String FML_HANDSHAKE_CHANNEL = "FML|HS";

    private String channel;
    private byte[] data;

    public ForgeHandshake() {
    }

    public ForgeHandshake(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.channel = readString(buf);
        this.data = new byte[buf.readableBytes()];
        buf.readBytes(this.data);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(channel, buf);
        buf.writeBytes(data);
    }

}
