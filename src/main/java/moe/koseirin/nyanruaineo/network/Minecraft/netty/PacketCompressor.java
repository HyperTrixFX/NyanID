package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.zip.Deflater;

/**
 * Compresses outbound frame payloads using the Minecraft compression format: a VarInt
 * uncompressed-length prefix (0 when the payload is sent verbatim) followed by the payload or its
 * zlib stream. Mirrors BungeeCord's {@code PacketCompressor}.
 */
public class PacketCompressor extends MessageToByteEncoder<ByteBuf> {

    private final int threshold;
    private final Deflater deflater = new Deflater();

    public PacketCompressor(int threshold) {
        this.threshold = threshold;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int origSize = msg.readableBytes();
        if (origSize < threshold) {
            DefinedPacket.writeVarInt(0, out);
            out.writeBytes(msg, msg.readerIndex(), origSize);
            return;
        }

        byte[] input = new byte[origSize];
        msg.readBytes(input);
        DefinedPacket.writeVarInt(origSize, out);

        deflater.setInput(input);
        deflater.finish();
        byte[] buffer = new byte[8192];
        while (!deflater.finished()) {
            int written = deflater.deflate(buffer);
            if (written > 0) {
                out.writeBytes(buffer, 0, written);
            } else if (!deflater.finished()) {
                // Output buffer was too small; grow it and keep going.
                buffer = new byte[buffer.length * 2];
            }
        }
        deflater.reset();
    }
}
