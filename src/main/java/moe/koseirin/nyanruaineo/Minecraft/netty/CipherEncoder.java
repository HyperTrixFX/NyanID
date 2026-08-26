package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import javax.crypto.Cipher;

/**
 * 使用登录期间协商的 AES/CFB8 流密码加密出站帧的有效负载。
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
