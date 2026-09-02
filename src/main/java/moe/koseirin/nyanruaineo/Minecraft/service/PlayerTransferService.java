package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.handler.ServerConnector;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Encapsulates moving a player from one backend (sub) server to another, mirroring BungeeCord's
 * server-connect flow. The target's online status is checked first (off the Netty event loop so a
 * probe against a dead backend never stalls other players), and an error is sent back when the
 * server is offline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerTransferService {

    private final MinecraftProxy proxy;
    private final BackendServerManager backendServerManager;

    /**
     * Checks the target server is online and transfers the player; when it is offline the
     * {@code reply} consumer receives the error message instead.
     */
    public void transferIfOnline(UserConnection user, BackendServer target, Consumer<String> reply) {
        backendServerManager.isOnlineAsync(target).thenAccept(online -> {
            if (!online) {
                reply.accept("§cServer " + target.getName() + " is offline!");
                return;
            }
            // Only report success when a transfer was actually started: a backend may deliver the
            // same Connect/ConnectOther once per online player connection, so the duplicate must
            // not emit a second "前往..." reply.
            if (transfer(user, target)) {
                reply.accept("§a前往 " + target.getUid() + "...");
            }
        });
    }

    /**
     * The actual server switch: pause the client, bump the server generation (so the old
     * backend's close cannot tear the client down), close the old backend, then connect to the
     * new one.
     *
     * @return {@code true} when a switch was started; {@code false} when it was skipped (player
     *         already disconnected, or a switch to some backend is already in progress).
     */
    public boolean transfer(UserConnection user, BackendServer target) {
        if (user.getChannel() == null || !user.getChannel().isActive()) {
            log.info("Player {} disconnected before the transfer to {} could start",
                    user.getUsername(), target.getName());
            return false;
        }

        // Idempotent check-and-set: with several players on one backend, the backend sends the
        // same Connect/ConnectOther plugin message through every player connection, and each
        // DownstreamBridge runs this branch. Only the first call may start the switch; the others
        // are dropped so they don't spawn a second racing ServerConnector.
        synchronized (user) {
            if (user.isSwitchingServer()) {
                log.debug("Player {} is already switching; ignoring duplicate transfer to {}",
                        user.getUsername(), target.getName());
                return false;
            }
            user.setSwitchingServer(true);
        }

        log.info("Transferring {} to {} ({}:{})", user.getUsername(), target.getName(),
                target.getHost(), target.getPort());

        // Leaving a Forge server: reset the client's FML handshake so the next backend starts
        // from a clean HELLO state (BungeeCord ServerConnector.handle(LoginSuccess)).
        ServerConnection old = user.getServer();
        if (old != null && old.isForgeServer() && user.getForgeClientHandler().isHandshakeComplete()) {
            user.getForgeClientHandler().resetHandshake();
        }

        // The switch flag is now set (above), so UpstreamBridge discards the client's stale GAME
        // frames until the new backend's JoinGame arrives (BungeeCord shouldHandle parity).
        user.getChannel().config().setAutoRead(false);
        user.nextServerGeneration();
        user.setServer(null);
        if (old != null && !old.isClosed()) {
            old.close();
        }
        new ServerConnector(proxy, user, target).connect();
        return true;
    }
}
