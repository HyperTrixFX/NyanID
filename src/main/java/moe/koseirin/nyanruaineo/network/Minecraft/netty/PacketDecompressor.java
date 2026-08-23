package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Decompresses inbound frame payloads using the Minecraft compression format. Mirrors BungeeCord's
 * {@code PacketDecompressor}.
 */
@Slf4j
public class PacketDecompressor extends ByteToMessageDecoder {

    private final Inflater inflater = new Inflater();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int uncompressedSize = DefinedPacket.readVarInt(in);

        if (uncompressedSize == 0) {
            // Uncompressed: the remainder of the frame is the packet.
            out.add(in.readRetainedSlice(in.readableBytes()));
            return;
        }

        byte[] input = new byte[in.readableBytes()];
        in.readBytes(input);

        byte[] output = new byte[uncompressedSize];
        inflater.setInput(input);
        try {
            int inflated = 0;
            while (inflated < uncompressedSize) {
                int written = inflater.inflate(output, inflated, uncompressedSize - inflated);
                if (written == 0) {
                    if (inflater.needsInput() || inflater.needsDictionary()) {
                        throw new CorruptedFrameException("Truncated compressed packet: inflated "
                                + inflated + " of " + uncompressedSize + " bytes");
                    }
                    continue;
                }
                inflated += written;
            }
        } catch (DataFormatException e) {
            throw new CorruptedFrameException("Failed to decompress packet", e);
        } finally {
            inflater.reset();
        }

        out.add(Unpooled.wrappedBuffer(output));
    }
}
