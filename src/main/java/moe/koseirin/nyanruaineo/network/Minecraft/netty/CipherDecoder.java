package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

/**
 * Decrypts inbound frame payloads using the AES/CFB8 stream cipher negotiated during login.
 * Mirrors BungeeCord's {@code CipherDecoder}.
 */
public class CipherDecoder extends ByteToMessageDecoder {

    private final Cipher cipher;

    public CipherDecoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readable = in.readableBytes();
        byte[] input = new byte[readable];
        in.readBytes(input);
        out.add(Unpooled.wrappedBuffer(cipher.update(input)));
    }
}
