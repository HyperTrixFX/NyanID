package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Protocol;

import java.util.List;

/**
 * Decodes a framed payload into a {@link DefinedPacket} when the current protocol state knows the
 * packet id, otherwise passes the raw frame through unchanged (used for transparent play-phase
 * relay). Mirrors BungeeCord's {@code MinecraftDecoder}.
 */
public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    private Protocol protocol;
    private final Direction direction;
    // Starts at 0 (not -1) so the Handshake packet — decoded before the real protocol version is
    // known — still matches the all-version packet registrations.
    private int protocolVersion = 0;

    public PacketDecoder(Protocol protocol, Direction direction) {
        this.protocol = protocol;
        this.direction = direction;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        int packetId;
        try {
            packetId = DefinedPacket.readVarInt(in);
        } catch (Exception ex) {
            in.resetReaderIndex();
            return;
        }

        if (protocol.hasPacket(direction, packetId, protocolVersion)) {
            DefinedPacket packet = protocol.createPacket(direction, packetId, protocolVersion);
            packet.read(in, protocolVersion);
            out.add(packet);
        } else {
            // Unknown packet for the current state: relay the frame verbatim.
            in.resetReaderIndex();
            out.add(in.retain());
        }
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }
}
