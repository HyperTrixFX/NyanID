package moe.koseirin.nyanruaineo.network.Minecraft.network.codec;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.MinecraftProtocolHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

import javax.crypto.Cipher;

public class PacketEncoder extends MessageToByteEncoder<Packet> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Packet msg, ByteBuf out) throws Exception {
        ByteBuf temp = ctx.alloc().buffer();
        try {
            VarIntCodec.writeVarInt(temp, msg.packetId());
            msg.encode(temp);

            byte[] plainBytes = new byte[temp.readableBytes()];
            temp.readBytes(plainBytes);

            Cipher encryptCipher = ctx.channel().attr(MinecraftProtocolHandler.ENCRYPTION_CIPHER).get();
            byte[] encryptedBytes = plainBytes;
            if (encryptCipher != null) {
                encryptedBytes = encryptCipher.doFinal(plainBytes);
            }

            VarIntCodec.writeVarInt(out, encryptedBytes.length);
            out.writeBytes(encryptedBytes);
        } finally {
            temp.release();
        }
    }
}