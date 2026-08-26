package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
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
    protected void decode(io.netty.channel.ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        in.markReaderIndex();

        int result = 0;
        int read = 0;
        boolean complete = false;
        for (int i = 0; i < MAX_VARINT_BYTES; i++) {
            if (!in.isReadable()) {
                in.resetReaderIndex();
                return;
            }
            byte b = in.readByte();
            read++;
            result |= (b & 0x7F) << (7 * i);
            if ((b & 0x80) == 0) {
                complete = true;
                break;
            }
        }

        if (!complete) {
            throw new CorruptedFrameException("VarInt21 frame length is too big");
        }
        if (result < 0) {
            throw new CorruptedFrameException("negative pre-length");
        }

        if (in.readableBytes() < result) {
            in.resetReaderIndex();
            return;
        }

        out.add(in.readRetainedSlice(result));
    }
}
