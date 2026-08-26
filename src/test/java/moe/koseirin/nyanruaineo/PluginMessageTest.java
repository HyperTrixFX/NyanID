package moe.koseirin.nyanruaineo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Wire-format round trips for the {@link PluginMessage} packet: the 1.13+ channel-name
 * modernisation (BungeeCord parity) and the VarInt-prefixed brand payload.
 */
class PluginMessageTest {

    @Test
    void legacyChannelRoundTrip() {
        PluginMessage message = new PluginMessage("BungeeCord", new byte[]{1, 2, 3});

        ByteBuf encoded = Unpooled.buffer();
        message.write(encoded, 47);                                   // 1.8: no modernise
        PluginMessage decoded = decode(encoded, Direction.TO_CLIENT, 47);

        assertEquals("BungeeCord", decoded.getTag());
        assertArrayEquals(new byte[]{1, 2, 3}, decoded.getData());
    }

    @Test
    void modernChannelRoundTrip() {
        PluginMessage message = new PluginMessage("BungeeCord", new byte[]{9, 9});

        ByteBuf encoded = Unpooled.buffer();
        message.write(encoded, 393);                                  // 1.13: modernise on wire
        // The wire carries "bungeecord:main"; decode turns it back into "BungeeCord".
        byte[] wireChannel = wireChannel(encoded, 393);
        PluginMessage decoded = decode(encoded, Direction.TO_CLIENT, 393);

        assertEquals(PluginMessage.BUNGEE_CHANNEL_LEGACY, decoded.getTag());
        assertEquals(PluginMessage.BUNGEE_CHANNEL_MODERN, new String(wireChannel, StandardCharsets.UTF_8));
    }

    @Test
    void brandPayloadIsVarIntPrefixedString() {
        // BungeeCord writes the brand as writeString(name), i.e. VarInt length + UTF-8 bytes.
        String brand = "NyanID (1.0)";
        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeString(brand, buf);
        byte[] payload = DefinedPacket.toArray(buf);
        buf.release();

        PluginMessage message = new PluginMessage("minecraft:brand", payload);

        ByteBuf encoded = Unpooled.buffer();
        message.write(encoded, 393);
        PluginMessage decoded = decode(encoded, Direction.TO_CLIENT, 393);
        assertEquals("minecraft:brand", decoded.getTag());
        assertEquals(brand, DefinedPacket.readString(Unpooled.wrappedBuffer(decoded.getData())));
    }

    @Test
    void registerPayloadIsRawNulJoined() {
        // The REGISTER payload is raw NUL-joined channel names (no length prefix).
        byte[] payload = "bungeecord:main\0minecraft:brand".getBytes(StandardCharsets.UTF_8);
        PluginMessage message = new PluginMessage("minecraft:register", payload);

        ByteBuf encoded = Unpooled.buffer();
        message.write(encoded, 393);
        PluginMessage decoded = decode(encoded, Direction.TO_CLIENT, 393);

        assertArrayEquals(payload, decoded.getData());
    }

    @Test
    void moderniseMapsBothWays() {
        assertEquals(PluginMessage.BUNGEE_CHANNEL_MODERN, PluginMessage.modernise(PluginMessage.BUNGEE_CHANNEL_LEGACY));
        assertEquals(PluginMessage.BUNGEE_CHANNEL_LEGACY, PluginMessage.modernise(PluginMessage.BUNGEE_CHANNEL_MODERN));
        assertEquals("minecraft:brand", PluginMessage.modernise("minecraft:brand"));
        assertTrue(PluginMessage.modernise("FML").startsWith("legacy:"));
    }

    @Test
    void payloadCapEnforcedAtTwoMiB() {
        // A proxy must relay modded custom payloads (frozen registry snapshots, config files, ...)
        // up to a generous 2 MiB in both directions; only absurdly large payloads are rejected.
        byte[] ok = new byte[0x200000 - 64];
        ByteBuf okBuf = Unpooled.wrappedBuffer(concat(varint("FML|HS"), ok));
        PluginMessage message = new PluginMessage();
        message.read(okBuf, Direction.TO_SERVER, 47);
        assertEquals(ok.length, message.getData().length);
        okBuf.release();

        byte[] big = new byte[0x200000 + 1];
        ByteBuf buf = Unpooled.wrappedBuffer(concat(varint("FML|HS"), big));
        try {
            new PluginMessage().read(buf, Direction.TO_SERVER, 47);
            throw new AssertionError("oversize payload must be rejected");
        } catch (IllegalArgumentException expected) {
            // Payload too large
        } finally {
            buf.release();
        }
    }

    private static PluginMessage decode(ByteBuf buf, Direction direction, int version) {
        try {
            PluginMessage message = new PluginMessage();
            message.read(buf, direction, version);
            return message;
        } finally {
            buf.release();
        }
    }

    private static byte[] wireChannel(ByteBuf encoded, int version) {
        ByteBuf probe = encoded.duplicate();
        int len = DefinedPacket.readVarInt(probe);
        byte[] channel = new byte[len];
        probe.readBytes(channel);
        return channel;
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static byte[] varint(String value) {
        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeString(value, buf);
        byte[] out = DefinedPacket.toArray(buf);
        buf.release();
        return out;
    }
}
