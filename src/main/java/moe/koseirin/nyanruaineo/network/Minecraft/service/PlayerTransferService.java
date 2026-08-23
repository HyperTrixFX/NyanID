package moe.koseirin.nyanruaineo.network.Minecraft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.handler.ServerConnector;
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
            reply.accept("§aConnecting to " + target.getName() + "...");
            transfer(user, target);
        });
    }

    /**
     * The actual server switch: pause the client, bump the server generation (so the old
     * backend's close cannot tear the client down), close the old backend, then connect to the
     * new one.
     */
    public void transfer(UserConnection user, BackendServer target) {
        if (user.getChannel() == null || !user.getChannel().isActive()) {
            log.info("Player {} disconnected before the transfer to {} could start",
                    user.getUsername(), target.getName());
            return;
        }
        log.info("Transferring {} to {} ({}:{})", user.getUsername(), target.getName(),
                target.getHost(), target.getPort());

        // The new backend repopulates the TabList from scratch.
        user.getTabListNames().clear();
        user.getChannel().config().setAutoRead(false);
        user.nextServerGeneration();
        ServerConnection old = user.getServer();
        user.setServer(null);
        if (old != null && !old.isClosed()) {
            old.close();
        }
        new ServerConnector(proxy, user, target).connect();
    }
}
