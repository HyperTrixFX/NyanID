package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Prepends the VarInt frame length to outgoing frame payloads. Mirrors BungeeCord's
 * {@code Varint21LengthFieldPrepender}.
 */
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int bodyLength = msg.readableBytes();
        DefinedPacket.writeVarInt(bodyLength, out);
        out.writeBytes(msg, msg.readerIndex(), bodyLength);
    }
}
