package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Query service for the proxy's backend (sub) servers: online status and per-server player
 * counts. Online checks reuse the {@link BackendServerManager} probe cache and run off the Netty
 * event loop.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServerStatusService {

    private final BackendServerManager backendServerManager;
    private final PlayerQueryService playerQueryService;

    /** Snapshot of one backend server's status. */
    public record ServerStatus(String name, String host, int port, boolean online, int playerCount) {
    }

    /**
     * Status of every configured server. Probes run in parallel on the manager's probe pool; the
     * returned future completes once all checks finished (offline servers take up to the probe
     * timeout, so call this from event-loop threads, not blocking ones).
     */
    public CompletableFuture<List<ServerStatus>> getStatusesAsync() {
        List<BackendServer> servers = backendServerManager.listServers();
        if (servers.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<ServerStatus>> futures = new ArrayList<>(servers.size());
        for (BackendServer server : servers) {
            futures.add(backendServerManager.isOnlineAsync(server).thenApply(online ->
                    new ServerStatus(server.getName(), server.getHost(), server.getPort(), online,
                            playerQueryService.countPlayersOn(server.getHost(), server.getPort()))));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(unused -> futures.stream().map(CompletableFuture::join).toList());
    }

    /** Blocking variant using the cached online state; prefer {@link #getStatusesAsync()}. */
    public List<ServerStatus> getStatuses() {
        return backendServerManager.listServers().stream()
                .map(server -> new ServerStatus(server.getName(), server.getHost(), server.getPort(),
                        backendServerManager.isOnline(server),
                        playerQueryService.countPlayersOn(server.getHost(), server.getPort())))
                .toList();
    }
}
