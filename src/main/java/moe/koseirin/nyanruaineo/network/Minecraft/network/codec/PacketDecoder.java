package moe.koseirin.nyanruaineo.network.Minecraft.network.codec;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.MinecraftProtocolHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.MinecraftPacketRegistry;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

import javax.crypto.Cipher;
import java.util.List;

@Slf4j
public class PacketDecoder extends ByteToMessageDecoder {
    private final MinecraftPacketRegistry registry;
    private final AttributeKey<ConnectionState> stateKey;

    public PacketDecoder(MinecraftPacketRegistry registry, AttributeKey<ConnectionState> stateKey) {
        this.registry = registry;
        this.stateKey = stateKey;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() == 0) return;
        in.markReaderIndex();

        int packetLength;
        try {
            packetLength = VarIntCodec.readVarInt(in);
        } catch (Exception e) {
            in.resetReaderIndex();
            return;
        }

        if (in.readableBytes() < packetLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] encryptedData = new byte[packetLength];
        in.readBytes(encryptedData);

        Cipher decryptCipher = ctx.channel().attr(MinecraftProtocolHandler.DECRYPTION_CIPHER).get();
        byte[] plainData;
        if (decryptCipher != null) {
            plainData = decryptCipher.doFinal(encryptedData);
        } else {
            plainData = encryptedData;
        }

        ByteBuf plainBuf = Unpooled.wrappedBuffer(plainData);
        try {
            int packetId = VarIntCodec.readVarInt(plainBuf);
            ConnectionState state = ctx.channel().attr(stateKey).get();
            Packet packet = registry.createPacket(state, packetId);
            packet.decode(plainBuf);
            out.add(packet);
        } finally {
            plainBuf.release();
        }
    }
}