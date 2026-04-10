package moe.koseirin.nyanruaineo.network.Minecraft.config;

import jakarta.annotation.PostConstruct;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.MinecraftPacketRegistry;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.handshake.HandshakePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.EncryptionResponsePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginStartPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.PingRequestPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.StatusRequestPacket;
import org.springframework.context.annotation.Configuration;

/*
 * @author KoseiRin_
 * awa
 */
@Configuration
public class MinecraftPacketRegistryConfig {

    private final MinecraftPacketRegistry minecraftPacketRegistry;

    public MinecraftPacketRegistryConfig(MinecraftPacketRegistry minecraftPacketRegistry) {
        this.minecraftPacketRegistry = minecraftPacketRegistry;
    }

    @PostConstruct
    public void registerPackets() {
        // Handshake state
        minecraftPacketRegistry.register(ConnectionState.HANDSHAKE, 0x00, HandshakePacket::new);

        // Status state
        minecraftPacketRegistry.register(ConnectionState.STATUS, 0x00, StatusRequestPacket::new);
        minecraftPacketRegistry.register(ConnectionState.STATUS, 0x01, PingRequestPacket::new);

        // Login state
        minecraftPacketRegistry.register(ConnectionState.LOGIN, 0x00, LoginStartPacket::new);
        minecraftPacketRegistry.register(ConnectionState.LOGIN, 0x01, EncryptionResponsePacket::new);
    }
}
