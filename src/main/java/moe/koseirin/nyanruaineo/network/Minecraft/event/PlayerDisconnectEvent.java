package moe.koseirin.nyanruaineo.network.Minecraft.event;

import java.util.UUID;

/**
 * Fired on the proxy when a player disconnects (or is disconnected), regardless of the reason.
 * The username is {@code null} when the client disconnected before completing the login.
 * Posted on the EventBus.
 */
public record PlayerDisconnectEvent(String username, UUID uuid) {
}
