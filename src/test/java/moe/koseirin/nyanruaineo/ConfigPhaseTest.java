package moe.koseirin.nyanruaineo;

import moe.koseirin.nyanruaineo.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Protocol;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.FinishConfiguration;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.LoginAcknowledged;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.StartConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The 1.20.2+ configuration phase: protocol-phase transitions and the BungeeCord packet-id maps
 * for a 1.21.1 (protocol 767) NeoForge client.
 */
class ConfigPhaseTest {

    private static final int V1_21_1 = 767;
    private static final int V1_20_2 = 764;
    private static final int V1_20_5 = 766;

    @Test
    void nextProtocolTransitions() {
        assertEquals(Protocol.CONFIGURATION, new LoginAcknowledged().nextProtocol());
        assertEquals(Protocol.CONFIGURATION, new StartConfiguration().nextProtocol());
        assertEquals(Protocol.GAME, new FinishConfiguration().nextProtocol());
        assertNull(new PluginMessage().nextProtocol());               // ordinary packets stay put
    }

    @Test
    void loginAcknowledgedId() {
        assertEquals(0x03, Protocol.LOGIN.getData(Direction.TO_SERVER)
                .getId(LoginAcknowledged.class, V1_21_1));
    }

    @Test
    void configurationPacketIds_1211() {
        // Serverbound (client → proxy → backend).
        assertEquals(0x02, Protocol.CONFIGURATION.getData(Direction.TO_SERVER)
                .getId(PluginMessage.class, V1_21_1));
        assertEquals(0x03, Protocol.CONFIGURATION.getData(Direction.TO_SERVER)
                .getId(FinishConfiguration.class, V1_21_1));
        // Clientbound (backend → proxy → client).
        assertEquals(0x01, Protocol.CONFIGURATION.getData(Direction.TO_CLIENT)
                .getId(PluginMessage.class, V1_21_1));
        assertEquals(0x03, Protocol.CONFIGURATION.getData(Direction.TO_CLIENT)
                .getId(FinishConfiguration.class, V1_21_1));
    }

    @Test
    void configurationPacketIds_1202and1205() {
        // 1.20.2 (764) uses the older configuration ids.
        assertEquals(0x01, Protocol.CONFIGURATION.getData(Direction.TO_SERVER)
                .getId(PluginMessage.class, V1_20_2));
        assertEquals(0x02, Protocol.CONFIGURATION.getData(Direction.TO_SERVER)
                .getId(FinishConfiguration.class, V1_20_2));
        assertEquals(0x00, Protocol.CONFIGURATION.getData(Direction.TO_CLIENT)
                .getId(PluginMessage.class, V1_20_2));
        assertEquals(0x02, Protocol.CONFIGURATION.getData(Direction.TO_CLIENT)
                .getId(FinishConfiguration.class, V1_20_2));
        // 1.20.5 (766) uses the modern configuration ids.
        assertEquals(0x02, Protocol.CONFIGURATION.getData(Direction.TO_SERVER)
                .getId(PluginMessage.class, V1_20_5));
        assertEquals(0x03, Protocol.CONFIGURATION.getData(Direction.TO_CLIENT)
                .getId(FinishConfiguration.class, V1_20_5));
    }

    @Test
    void startConfigurationIds_1211() {
        assertEquals(0x69, Protocol.GAME.getData(Direction.TO_CLIENT)
                .getId(StartConfiguration.class, V1_21_1));
        assertEquals(0x0C, Protocol.GAME.getData(Direction.TO_SERVER)
                .getId(StartConfiguration.class, V1_21_1));
    }
}
