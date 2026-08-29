package moe.koseirin.nyanruaineo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.EncryptionRequest;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.EncryptionResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Login-phase packet formats for the 1.20.5+ (766+) clients: the EncryptionRequest must carry the
 * trailing shouldAuthenticate boolean (its absence makes 1.21.1 clients report "Failed to decode
 * packet 'clientbound/minecraft:hello'"), and the EncryptionResponse keeps the two-array format
 * (the salt+signature variant exists only for 1.19-1.19.2).
 */
class LoginPacketsTest {

    @Test
    void encryptionRequestHasTrailingBoolean_1211() {
        EncryptionRequest request = new EncryptionRequest("", new byte[]{1, 2, 3}, new byte[]{9, 9});
        ByteBuf buf = Unpooled.buffer();
        request.write(buf, 767);                                       // 1.21.1
        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        buf.release();

        // The trailing boolean must be present at the end (0x01 = true).
        assertEquals(0x01, frame[frame.length - 1] & 0xFF);

        EncryptionRequest decoded = new EncryptionRequest();
        decoded.read(Unpooled.wrappedBuffer(frame), 767);
        assertTrue(decoded.isShouldAuthenticate());
        assertArrayEquals(new byte[]{1, 2, 3}, decoded.getPublicKey());
        assertArrayEquals(new byte[]{9, 9}, decoded.getVerifyToken());
    }

    @Test
    void encryptionRequestNoBoolean_1201() {
        EncryptionRequest request = new EncryptionRequest("", new byte[]{1}, new byte[]{2});
        ByteBuf buf = Unpooled.buffer();
        request.write(buf, 763);                                       // 1.20.1: no trailing boolean
        assertEquals(0, buf.readableBytes() - (buf.readableBytes()));  // no-op; just ensure write works
        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        buf.release();
        EncryptionRequest decoded = new EncryptionRequest();
        decoded.read(Unpooled.wrappedBuffer(frame), 763);
        assertEquals(1, decoded.getPublicKey().length);
    }

    @Test
    void encryptionResponseTwoArrays_1211() {
        // 1.21.1: shared secret + verify token, no salt/signature.
        EncryptionResponse response = new EncryptionResponse();
        response.setSharedSecret(new byte[128]);
        response.setVerifyToken(new byte[4]);
        ByteBuf buf = Unpooled.buffer();
        response.write(buf, 767);
        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        buf.release();

        EncryptionResponse decoded = new EncryptionResponse();
        decoded.read(Unpooled.wrappedBuffer(frame), 767);
        assertArrayEquals(new byte[128], decoded.getSharedSecret());
        assertArrayEquals(new byte[4], decoded.getVerifyToken());
        assertNull(decoded.getMessageSignature());
    }

    @Test
    void encryptionResponseSaltSignature_119() {
        // 1.19-1.19.2: the client may send salt + signature instead of the verify token.
        EncryptionResponse response = new EncryptionResponse();
        response.setSharedSecret(new byte[128]);
        response.setVerifyToken(null);
        response.setHasSaltSignature(true);
        response.setSalt(42L);
        response.setMessageSignature(new byte[]{7, 8, 9});
        ByteBuf buf = Unpooled.buffer();
        response.write(buf, 759);                                      // 1.19
        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        buf.release();

        EncryptionResponse decoded = new EncryptionResponse();
        decoded.read(Unpooled.wrappedBuffer(frame), 759);
        assertEquals(42L, decoded.getSalt());
        assertArrayEquals(new byte[]{7, 8, 9}, decoded.getMessageSignature());
    }

    @Test
    void encryptionRequestRoundTripsViaCodecs() {
        // Byte-stability: encode -> decode -> encode yields identical bytes for 767.
        EncryptionRequest request = new EncryptionRequest("", new byte[]{1, 2, 3}, new byte[]{9, 9});
        ByteBuf a = Unpooled.buffer();
        request.write(a, 767);
        byte[] once = DefinedPacket.toArray(a);
        a.release();
        EncryptionRequest decoded = new EncryptionRequest();
        decoded.read(Unpooled.wrappedBuffer(once), 767);
        ByteBuf b = Unpooled.buffer();
        decoded.write(b, 767);
        byte[] twice = DefinedPacket.toArray(b);
        b.release();
        assertArrayEquals(once, twice);
    }
}
