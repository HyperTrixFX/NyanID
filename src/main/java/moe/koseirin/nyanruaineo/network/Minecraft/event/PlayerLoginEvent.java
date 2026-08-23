package moe.koseirin.nyanruaineo.network.Minecraft.event;

import java.util.UUID;

/**
 * Fired on the proxy once the client has completed authentication (Mojang online-mode auth or
 * offline UUID generation), before the backend connection is established. Posted on the EventBus.
 */
public record PlayerLoginEvent(String username, UUID uuid, int protocolVersion, String requestedServer, String ip) {
}
