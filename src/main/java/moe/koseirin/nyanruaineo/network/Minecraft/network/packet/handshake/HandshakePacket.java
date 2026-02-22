package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.handshake;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

@Data
@NoArgsConstructor
public class HandshakePacket implements Packet {
    private int protocolVersion;
    private String serverAddress;
    private int serverPort;
    private int nextState; // 1: status, 2: login

    public HandshakePacket(int protocolVersion, String serverAddress, int serverPort, int nextState) {
        this.protocolVersion = protocolVersion;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.nextState = nextState;
    }

    @Override
    public int packetId() { return 0x00; }

    @Override
    public void encode(ByteBuf buf) {
        VarIntCodec.writeVarInt(buf, protocolVersion);
        VarIntCodec.writeString(buf, serverAddress);
        buf.writeShort(serverPort);
        VarIntCodec.writeVarInt(buf, nextState);
    }

    @Override
    public void decode(ByteBuf buf) {
        this.protocolVersion = VarIntCodec.readVarInt(buf);
        this.serverAddress = VarIntCodec.readString(buf);
        this.serverPort = buf.readUnsignedShort();
        this.nextState = VarIntCodec.readVarInt(buf);
    }

}