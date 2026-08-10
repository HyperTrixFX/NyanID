package moe.koseirin.nyanruaineo.websocket;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.forge.ForgeModListPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.handshake.HandshakePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginStartPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.PingRequestPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.StatusRequestPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.BindAccountPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.CheckBindPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.HeartbeatResponsePacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.UpdateOnlinePacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.CheckBindResponsePacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.HeartbeatPacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.S01Packet;
import moe.koseirin.nyanruaineo.network.Packet.Server.UpdateOnlineResponsePacket;
import moe.koseirin.nyanruaineo.network.utils.PacketRegistry;
import moe.koseirin.nyanruaineo.websocket.Handler.BungeeWebSocketHandler;
import moe.koseirin.nyanruaineo.websocket.Interceptor.BungeeAuthHandshakeInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BungeeWebSocketHandler bungeeWebSocketHandler;
    private final PacketRegistry packetRegistry;

    public WebSocketConfig(BungeeWebSocketHandler bungeeWebSocketHandler, PacketRegistry packetRegistry) {
        this.bungeeWebSocketHandler = bungeeWebSocketHandler;
        this.packetRegistry = packetRegistry;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(bungeeWebSocketHandler, "/api/zako/v3/websocket/bungee")
                .addInterceptors(new BungeeAuthHandshakeInterceptor())
                .setAllowedOrigins("*");
    }

    @PostConstruct
    public void registerPackets() {
        packetRegistry.register(0x01, HeartbeatPacket::new);
        packetRegistry.register(0x81, HeartbeatResponsePacket::new);
        packetRegistry.register(0x03, BindAccountPacket::new);
        packetRegistry.register(0x04, CheckBindPacket::new);
        packetRegistry.register(0x02, UpdateOnlinePacket::new);
        packetRegistry.register(0x83, CheckBindResponsePacket::new);
        packetRegistry.register(0x84, S01Packet::new);
        packetRegistry.register(0x82, UpdateOnlineResponsePacket::new);

    }


}