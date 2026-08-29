package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

import java.util.zip.Deflater;

/**
 * 这个压缩器负责压缩发往客户端的数据包内容，遵循 Minecraft 自己的压缩格式：
 * 开头是一个 VarInt 类型的前缀，表示未压缩时的数据长度（如果数据未压缩、直接发送，那这个前缀就是 0），
 * 后面跟着要么是原始数据，要么是它的 zlib 压缩数据流。
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
