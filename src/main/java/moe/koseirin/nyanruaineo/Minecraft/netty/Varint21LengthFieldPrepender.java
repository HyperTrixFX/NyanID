package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 这个编码器负责在发往客户端的帧数据前面加上一个 VarInt 类型的长度前缀，
 * 告诉对方这个帧有多长。
 */
public class Varint21LengthFieldPrepender extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int bodyLength = msg.readableBytes();
        DefinedPacket.writeVarInt(bodyLength, out);
        out.writeBytes(msg, msg.readerIndex(), bodyLength);
    }
}
