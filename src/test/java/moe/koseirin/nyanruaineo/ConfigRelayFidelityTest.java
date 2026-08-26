package moe.koseirin.nyanruaineo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end byte fidelity of a 1.21.1 (protocol 767) NeoForge configuration exchange through the
 * proxy codecs: the backend's config-phase custom payloads (e.g. {@code minecraft:hello}) must
 * reach the client with identical packet id, channel and payload.
 */
class ConfigRelayFidelityTest {

    private static final int V = 767;

    /** Decodes one frame exactly like the backend channel (CONFIG, clientbound). */
    private static Object decodeBackendFrame(byte[] frame) {
        ByteBuf in = Unpooled.wrappedBuffer(frame);
        DefinedPacket packet = Protocol.CONFIGURATION.getData(Direction.TO_CLIENT).createPacket(
                DefinedPacket.readVarInt(in), V);
        packet.read(in, Direction.TO_CLIENT, V);
        return packet;
    }

    /** Encodes one packet exactly like the front-end channel (CONFIG, clientbound). */
    private static byte[] encodeFrontend(DefinedPacket packet) {
        ByteBuf out = Unpooled.buffer();
        int id = Protocol.CONFIGURATION.getData(Direction.TO_CLIENT).getId(packet.getClass(), V);
        DefinedPacket.writeVarInt(id, out);
        packet.write(out, V);
        byte[] bytes = new byte[out.readableBytes()];
        out.readBytes(bytes);
        out.release();
        return bytes;
    }

    private static byte[] frame(String tag, byte[] payload) {
        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeVarInt(0x01, buf);                          // config clientbound custom payload
        DefinedPacket.writeString(tag, buf);
        buf.writeBytes(payload);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        return bytes;
    }

    @Test
    void helloPayloadReachesClientByteIdentical() {
        byte[] payload = {0x00};                                       // NeoForge hello carries one byte
        byte[] frame = frame("minecraft:hello", payload);

        Object decoded = decodeBackendFrame(frame);
        assertEquals(PluginMessage.class, decoded.getClass());
        assertEquals("minecraft:hello", ((PluginMessage) decoded).getTag());
        assertArrayEquals(payload, ((PluginMessage) decoded).getData());

        byte[] reencoded = encodeFrontend((DefinedPacket) decoded);
        assertArrayEquals(frame, reencoded, "backend frame must reach the client byte-identical");
    }

    @Test
    void registryDataPayloadLargeRoundTrip() {
        // NeoForge registry sync can be sizeable; the payload must survive verbatim.
        byte[] payload = new byte[4096];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }
        byte[] frame = frame("minecraft:registry_data", payload);

        Object decoded = decodeBackendFrame(frame);
        assertArrayEquals(payload, ((PluginMessage) decoded).getData());
        assertArrayEquals(frame, encodeFrontend((DefinedPacket) decoded));
    }

    @Test
    void modernNamespacedChannelsAreNeverTransformed() {
        for (String tag : new String[]{"minecraft:hello", "minecraft:registry_data",
                "minecraft:select_known_packs", "minecraft:update_tags", "fml:handshake"}) {
            byte[] frame = frame(tag, new byte[]{1, 2, 3});
            Object decoded = decodeBackendFrame(frame);
            assertEquals(tag, ((PluginMessage) decoded).getTag(), "channel must survive verbatim: " + tag);
            assertArrayEquals(frame, encodeFrontend((DefinedPacket) decoded), "frame must survive: " + tag);
        }
    }

    @Test
    void configServerboundHelloSurvivesClientToBackend() {
        // Client's hello response: config serverbound custom payload id 0x02.
        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeVarInt(0x02, buf);
        DefinedPacket.writeString("minecraft:hello", buf);
        buf.writeByte(0x00);
        byte[] frame = new byte[buf.readableBytes()];
        buf.readBytes(frame);
        buf.release();

        // Frontend decode (TO_SERVER) then backend encode (TO_SERVER).
        ByteBuf in = Unpooled.wrappedBuffer(frame);
        DefinedPacket packet = Protocol.CONFIGURATION.getData(Direction.TO_SERVER).createPacket(
                DefinedPacket.readVarInt(in), V);
        packet.read(in, Direction.TO_SERVER, V);

        ByteBuf out = Unpooled.buffer();
        int id = Protocol.CONFIGURATION.getData(Direction.TO_SERVER).getId(packet.getClass(), V);
        DefinedPacket.writeVarInt(id, out);
        packet.write(out, V);
        byte[] reencoded = new byte[out.readableBytes()];
        out.readBytes(reencoded);
        out.release();

        assertArrayEquals(frame, reencoded, "client hello response must reach the backend byte-identical");
    }
}
