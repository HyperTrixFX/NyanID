package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;

/**
 * Encrypts outbound frame payloads using the AES/CFB8 stream cipher negotiated during login.
 * Mirrors BungeeCord's {@code CipherEncoder}.
 */
public class CipherEncoder extends MessageToByteEncoder<ByteBuf> {

    private final Cipher cipher;

    public CipherEncoder(Cipher cipher) {
        this.cipher = cipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        int readable = msg.readableBytes();
        byte[] input = new byte[readable];
        msg.readBytes(input);
        out.writeBytes(cipher.update(input));
    }
}
