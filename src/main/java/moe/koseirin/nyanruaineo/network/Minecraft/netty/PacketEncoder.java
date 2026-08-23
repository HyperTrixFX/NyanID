package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.ReferenceCountUtil;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Protocol;

import java.util.List;

/**
 * Encodes a {@link DefinedPacket} into a framed payload, or passes a raw {@link ByteBuf} through
 * unchanged. Mirrors BungeeCord's {@code MinecraftEncoder}.
 */
public class PacketEncoder extends MessageToMessageEncoder<Object> {

    private Protocol protocol;
    private final Direction direction;
    // Starts at 0 (not -1) so packets encoded before the handshake is read still resolve their ids.
    private int protocolVersion = 0;

    public PacketEncoder(Protocol protocol, Direction direction) {
        this.protocol = protocol;
        this.direction = direction;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {
        if (msg instanceof DefinedPacket packet) {
            ByteBuf buf = ctx.alloc().buffer();
            int packetId = protocol.getId(direction, packet.getClass(), protocolVersion);
            DefinedPacket.writeVarInt(packetId, buf);
            packet.write(buf, protocolVersion);
            out.add(buf);
        } else {
            out.add(ReferenceCountUtil.retain(msg));
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
