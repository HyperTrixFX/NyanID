package moe.koseirin.nyanruaineo.network.Minecraft.netty;

import io.netty.channel.ChannelPipeline;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.ProtocolConstants;

import javax.crypto.Cipher;

/**
 * Builds the shared Minecraft pipeline, mirroring BungeeCord's {@code PipelineUtils}.
 */
public final class PipelineUtils {

    private PipelineUtils() {
    }

    /**
     * Builds a front-end (client-facing) pipeline. Inbound is decoded in {@link Direction#TO_SERVER}
     * and outbound is encoded in {@link Direction#TO_CLIENT}.
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
     * Builds a back-end (server-facing) pipeline. Inbound is decoded in {@link Direction#TO_CLIENT}
     * and outbound is encoded in {@link Direction#TO_SERVER}.
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
     * Enables AES/CFB8 encryption on a front-end channel after the login secret is negotiated.
     * <p>
     * Vanilla (and BungeeCord) apply the cipher to the WHOLE framed stream, including the VarInt
     * length prefix: inbound is {@code decrypt(raw) -> frame-decoder}, outbound is
     * {@code frame-prepender -> encrypt(framed)}. The cipher handlers are therefore inserted BEFORE
     * the frame codecs, not after.
     */
    public static void enableEncryption(ChannelPipeline pipeline, Cipher encryptCipher, Cipher decryptCipher) {
        pipeline.addBefore(ProtocolConstants.FRAME_PREPENDER, ProtocolConstants.CIPHER_ENCODER,
                new CipherEncoder(encryptCipher));
        pipeline.addBefore(ProtocolConstants.FRAME_DECODER, ProtocolConstants.CIPHER_DECODER,
                new CipherDecoder(decryptCipher));
    }
}
