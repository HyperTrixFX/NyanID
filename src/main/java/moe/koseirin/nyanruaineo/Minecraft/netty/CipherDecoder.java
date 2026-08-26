package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.crypto.Cipher;
import java.util.List;

/**
 * 使用登录期间协商的 AES/CFB8 流密码解密入站帧的有效负载。
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
