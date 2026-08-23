package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

import java.util.List;

/**
 * Splits the byte stream into Minecraft frames: a VarInt length prefix (at most 3 bytes, hence
 * "21" bits) followed by the frame payload. Mirrors BungeeCord's {@code Varint21FrameDecoder}.
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
