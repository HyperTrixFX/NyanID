package moe.koseirin.nyanruaineo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.JoinGame;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.Respawn;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Byte-perfect round trips of the 1.20.2+ (764+) JoinGame and Respawn packets, which the
 * 1.20.2+ server-switch flow re-encodes. Encoding must be stable: encode → decode → encode
 * yields identical bytes.
 */
class JoinGameSwitchTest {

    private static JoinGame sample(int version) {
        JoinGame login = new JoinGame();
        login.setEntityId(42);
        login.setHardcore(true);
        login.setGameMode((short) 1);
        login.setPreviousGameMode((short) 0);
        login.setWorldNames(List.of("minecraft:overworld", "minecraft:the_nether"));
        login.setDimension(version >= 766 ? 0 : "minecraft:overworld");
        login.setWorldName("minecraft:overworld");
        login.setSeed(123456789L);
        login.setMaxPlayers(100);
        login.setViewDistance(10);
        login.setSimulationDistance(10);
        login.setReducedDebugInfo(false);
        login.setNormalRespawn(true);
        login.setLimitedCrafting(false);
        login.setDebug(false);
        login.setFlat(false);
        login.setDeathDimension(null);
        login.setPortalCooldown(0);
        login.setSeaLevel(63);
        login.setOnlineMode(false);
        login.setSecureProfile(true);
        return login;
    }

    private static byte[] encode(JoinGame login, int version) {
        ByteBuf buf = Unpooled.buffer();
        login.write(buf, version);
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        buf.release();
        return out;
    }

    private static JoinGame decode(byte[] bytes, int version) {
        ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        JoinGame login = new JoinGame();
        login.read(buf, version);
        buf.release();
        return login;
    }

    @Test
    void joinGameRoundTrip_1202() {
        assertRoundTrip(764);
    }

    @Test
    void joinGameRoundTrip_1211() {
        assertRoundTrip(767);
    }

    @Test
    void joinGameRoundTrip_1212() {
        assertRoundTrip(768);
    }

    private void assertRoundTrip(int version) {
        JoinGame login = sample(version);
        byte[] once = encode(login, version);
        JoinGame decoded = decode(once, version);
        byte[] twice = encode(decoded, version);
        assertArrayEquals(once, twice, "JoinGame re-encode must be byte-identical at v" + version);

        // Field fidelity across the round trip.
        assertEquals(42, decoded.getEntityId());
        assertTrue(decoded.isHardcore());
        assertEquals((short) 1, decoded.getGameMode());
        assertEquals(version >= 766 ? 0 : "minecraft:overworld", decoded.getDimension());
        assertEquals("minecraft:overworld", decoded.getWorldName());
        assertEquals(123456789L, decoded.getSeed());
        assertEquals(100, decoded.getMaxPlayers());
        assertEquals(10, decoded.getViewDistance());
        assertEquals(10, decoded.getSimulationDistance());
        assertTrue(decoded.isNormalRespawn());
        if (version >= 768) {                                          // sea level only on the wire from 1.21.2
            assertEquals(63, decoded.getSeaLevel());
        }
        if (version >= 766) {
            assertTrue(decoded.isSecureProfile());
        }
        assertEquals(List.of("minecraft:overworld", "minecraft:the_nether"),
                new java.util.ArrayList<>(decoded.getWorldNames()));
    }

    @Test
    void joinGameToRespawnCarriesFields() {
        JoinGame login = sample(767);
        Respawn respawn = login.toRespawn();

        ByteBuf buf = Unpooled.buffer();
        respawn.write(buf, 767);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();

        Respawn parsed = new Respawn();
        parsed.read(Unpooled.wrappedBuffer(bytes), 767);
        // The 1.20.5+ dimension is a VarInt id; the Respawn payload carries it verbatim.
        assertEquals(0, parsed.getDimension());
        assertEquals("minecraft:overworld", parsed.getWorldName());
        assertEquals(123456789L, parsed.getSeed());
        assertEquals((short) 1, parsed.getGameMode());
        assertEquals((short) 0, parsed.getPreviousGameMode());
        assertEquals(0, parsed.getPortalCooldown());
        // 1.21.1 has no sea level field; 1.21.2+ does.
        Respawn respawn768 = login.toRespawn();
        ByteBuf buf768 = Unpooled.buffer();
        respawn768.write(buf768, 768);
        byte[] bytes768 = new byte[buf768.readableBytes()];
        buf768.readBytes(bytes768);
        buf768.release();
        Respawn parsed768 = new Respawn();
        parsed768.read(Unpooled.wrappedBuffer(bytes768), 768);
        assertEquals(63, parsed768.getSeaLevel());

        // Byte-stability of the Respawn re-encode as well.
        assertArrayEquals(bytes768, encodeRespawn(parsed768, 768));
    }

    private static byte[] encodeRespawn(Respawn respawn, int version) {
        ByteBuf buf = Unpooled.buffer();
        respawn.write(buf, version);
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        buf.release();
        return out;
    }
}
