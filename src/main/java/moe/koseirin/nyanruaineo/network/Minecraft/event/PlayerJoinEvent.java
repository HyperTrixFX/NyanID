package moe.koseirin.nyanruaineo.network.Minecraft.event;

import java.util.UUID;

/**
 * Fired on the proxy when the backend has accepted the login and the play-phase relay begins.
 * Posted on the EventBus.
 */
public record PlayerJoinEvent(String username, UUID uuid, int protocolVersion, String serverHost, int serverPort) {
}
