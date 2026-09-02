package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

/**
 * 这个解码器负责把原始字节流切分成一个个 Minecraft 帧。
 * 每帧的格式是：开头是一个 VarInt 类型的前缀（最多占用 3 个字节，也就是“21”位），用来表示后面跟着的有效负载的长度。
 */
public class Varint21FrameDecoder extends ByteToMessageDecoder {

    private static final int MAX_VARINT_BYTES = 3;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!ctx.channel().isActive()) {
            in.skipBytes(in.readableBytes());
            return;
        }

        in.markReaderIndex();

        int length = 0;
        for (int i = 0; i < MAX_VARINT_BYTES; i++) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }

            byte b = in.readByte();
            length |= (b & 0x7F) << (7 * i);

            if (b >= 0) {
                if (length == 0) {
                    throw new CorruptedFrameException("Empty Packet!");
                }

                if (in.readableBytes() < length) {
                    in.resetReaderIndex();
                    return;
                }

                if (in.hasMemoryAddress()) {
                    out.add(in.readRetainedSlice(length));
                } else {
                    ByteBuf dst = ctx.alloc().directBuffer(length);
                    in.readBytes(dst);
                    out.add(dst);
                }
                return;
            }
        }
        throw new CorruptedFrameException("length wider than 21-bit");
    }
}
