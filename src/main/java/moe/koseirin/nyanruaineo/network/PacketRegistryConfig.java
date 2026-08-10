package moe.koseirin.nyanruaineo.network;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.MinecraftPacketRegistry;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.forge.ForgeModListPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.handshake.HandshakePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginStartPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginSuccessPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.PingRequestPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.PingResponsePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.StatusRequestPacket;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PacketRegistryConfig {

    private final MinecraftPacketRegistry minecraftPacketRegistry;

    public PacketRegistryConfig(MinecraftPacketRegistry minecraftPacketRegistry) {
        this.minecraftPacketRegistry = minecraftPacketRegistry;
    }

    @PostConstruct
    public void registerPackets() {
        // Handshake 状态
        // 握手阶段 (Handshaking)
        minecraftPacketRegistry.register(ConnectionState.HANDSHAKE, 0x00, HandshakePacket::new);

        // 状态查询阶段 (Status)
        minecraftPacketRegistry.register(ConnectionState.STATUS, 0x00, StatusRequestPacket::new);
        minecraftPacketRegistry.register(ConnectionState.STATUS, 0x01, PingRequestPacket::new);

        // 登录阶段 (Login)
        minecraftPacketRegistry.register(ConnectionState.LOGIN, 0x00, LoginStartPacket::new);
        // Forge 客户端在登录阶段发送 Mod 列表 (0xFC)
        minecraftPacketRegistry.register(ConnectionState.LOGIN, 0xFC, () -> new ForgeModListPacket(false));
//        minecraftPacketRegistry.register(ConnectionState.LOGIN, 0x01, EncryptionResponsePacket::new);
    }
}