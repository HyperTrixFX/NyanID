package moe.koseirin.nyanruaineo.network.Minecraft.event;

/**
 * Fired on the proxy when a client performs a server list ping (status request). Posted on the
 * EventBus.
 */
public record ProxyPingEvent(int protocolVersion, String ip) {
}
