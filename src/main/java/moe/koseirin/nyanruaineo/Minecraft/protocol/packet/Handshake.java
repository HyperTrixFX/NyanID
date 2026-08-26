package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * Clientbound 握手数据包（0x00，握手状态）。
 */
@Setter
@Getter
public class Handshake extends DefinedPacket {

    private int protocolVersion;
    private String host;
    private int port;
    /** 1 = status, 2 = login. */
    private int requestedProtocol;

    public Handshake() {
    }

    public Handshake(int protocolVersion, String host, int port, int requestedProtocol) {
        this.protocolVersion = protocolVersion;
        this.host = host;
        this.port = port;
        this.requestedProtocol = requestedProtocol;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.protocolVersion = readVarInt(buf);
        this.host = readString(buf);
        this.port = buf.readUnsignedShort();
        this.requestedProtocol = readVarInt(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(this.protocolVersion, buf);
        writeString(this.host, buf);
        buf.writeShort(this.port);
        writeVarInt(this.requestedProtocol, buf);
    }

}
