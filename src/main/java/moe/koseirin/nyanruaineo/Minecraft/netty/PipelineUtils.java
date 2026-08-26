package moe.koseirin.nyanruaineo.Minecraft.netty;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.ChannelPipeline;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.Minecraft.protocol.ProtocolConstants;

import javax.crypto.Cipher;

/**
 * 这个工具类负责构建 Minecraft 协议处理所需的共享 Netty 管道。
 */
public final class PipelineUtils {

    private PipelineUtils() {
    }

    /**
     * 这个方法构建的是面向客户端的前端 Netty 管道。
     * 从客户端收到的数据（入站）会按照 {@link Direction#TO_SERVER} 的方向进行解码，
     * 而发往客户端的数据（出站）会按照 {@link Direction#TO_CLIENT} 的方向进行编码。
     */
    public static HandlerBoss initFrontendPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(ProtocolConstants.FRAME_PREPENDER, new Varint21LengthFieldPrepender());
        pipeline.addLast(ProtocolConstants.FRAME_DECODER, new Varint21FrameDecoder());
        pipeline.addLast(ProtocolConstants.PACKET_DECODER, new PacketDecoder(Protocol.HANDSHAKE, Direction.TO_SERVER));
        pipeline.addLast(ProtocolConstants.PACKET_ENCODER, new PacketEncoder(Protocol.HANDSHAKE, Direction.TO_CLIENT));
        HandlerBoss boss = new HandlerBoss();
        pipeline.addLast(ProtocolConstants.HANDLER, boss);
        return boss;
    }

    /**
     * 这个方法构建的是面向后端服务器（Backend Server）的管道。
     * 从后端服务器收到的数据（入站）会按照 {@link Direction#TO_CLIENT} 的方向进行解码，
     * 而发往后端服务器的数据（出站）会按照 {@link Direction#TO_SERVER} 的方向进行编码。
     */
    public static HandlerBoss initBackendPipeline(ChannelPipeline pipeline) {
        pipeline.addLast(ProtocolConstants.FRAME_PREPENDER, new Varint21LengthFieldPrepender());
        pipeline.addLast(ProtocolConstants.FRAME_DECODER, new Varint21FrameDecoder());
        // Start in HANDSHAKE so the ServerConnector can emit the Handshake packet first; it then
        // switches both codecs to LOGIN before sending the LoginRequest.
        pipeline.addLast(ProtocolConstants.PACKET_DECODER, new PacketDecoder(Protocol.HANDSHAKE, Direction.TO_CLIENT));
        pipeline.addLast(ProtocolConstants.PACKET_ENCODER, new PacketEncoder(Protocol.HANDSHAKE, Direction.TO_SERVER));
        HandlerBoss boss = new HandlerBoss();
        pipeline.addLast(ProtocolConstants.HANDLER, boss);
        return boss;
    }

    /**
     * 在登录密钥协商完成后，为前端通道启用 AES/CFB8 加密。
     * <p>
     * 原版（及 BungeeCord）将密码应用于整个帧流，包括 VarInt 长度前缀：
     * 入站方向为 {@code 解密(原始数据) -> 帧解码器}，出站方向为
     * {@code 帧前置处理器 -> 加密(已组帧数据)}。因此，密码处理器必须插入在
     * 帧编解码器之前，而非之后。
     */
    public static void enableEncryption(ChannelPipeline pipeline, Cipher encryptCipher, Cipher decryptCipher) {
        pipeline.addBefore(ProtocolConstants.FRAME_PREPENDER, ProtocolConstants.CIPHER_ENCODER,
                new CipherEncoder(encryptCipher));
        pipeline.addBefore(ProtocolConstants.FRAME_DECODER, ProtocolConstants.CIPHER_DECODER,
                new CipherDecoder(decryptCipher));
    }
}
