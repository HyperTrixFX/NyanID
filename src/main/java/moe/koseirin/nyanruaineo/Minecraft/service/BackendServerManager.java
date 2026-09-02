package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Resolves which backend (sub) server a connection should be routed to, looks servers up by name
 * for the {@code /server} command and tracks whether each backend is online. Routing keys off the
 * Handshake server address (matched against a configured backend's name or host) and falls back
 * to the configured default server, then to the highest-priority server. The list is read from
 * {@code proxy.backend.servers} at every lookup, so it can be adjusted at runtime without a
 * restart.
 */
@Slf4j
@Component
public class BackendServerManager {

    /** How long a successful or failed online probe is reused before probing again. */
    private static final long ONLINE_CACHE_MILLIS = 5000;

    private final ProxyProperties properties;

    /** Probes run off the Netty event loop so a dead backend never stalls player traffic. */
    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "backend-online-probe");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, OnlineStatus> onlineCache = new ConcurrentHashMap<>();

    public BackendServerManager(ProxyProperties properties) {
        this.properties = properties;
    }

    /**
     * Resolves the backend for an incoming connection, or {@code null} when no server is configured.
     * <ol>
     *   <li>Match the requested handshake address against a server name or host;</li>
     *   <li>fall back to the configured default server (matched by name or uid);</li>
     *   <li>otherwise the highest-priority server in the list.</li>
     * </ol>
     * Entries without a usable host/port are skipped for routing. The old single-backend
     * {@code proxy.backend.host/port} keys are no longer used.
     */
    public BackendServer select(String requestedAddress) {
        List<BackendServer> servers = properties.getBackendServers();
        if (servers.isEmpty()) {
            return null;
        }

        // 1. Match the requested handshake address against a server name or host.
        if (requestedAddress != null && !requestedAddress.isBlank()) {
            for (BackendServer server : servers) {
                if (server.getName().equalsIgnoreCase(requestedAddress)
                        || server.getHost().equalsIgnoreCase(requestedAddress)) {
                    return server;
                }
            }
        }

        // 2. Fall back to the configured default server (matched by name or uid).
        String defaultServer = properties.getServerListConfig().getDefaultServer();
        if (defaultServer != null && !defaultServer.isBlank()) {
            for (BackendServer server : servers) {
                if (server.getName().equalsIgnoreCase(defaultServer)
                        || (server.getUid() != null && server.getUid().equalsIgnoreCase(defaultServer))) {
                    return server;
                }
            }
        }

        // 3. Otherwise the highest-priority server.
        return servers.stream().min(java.util.Comparator.comparingInt(BackendServer::getPriority)).orElse(null);
    }

    /**
     * Looks a server up by name (case-insensitive) for the {@code /server} command, or {@code null}
     * when no such server exists. The lookup covers every configured entry, even ones without a
     * usable host/port, so the command can report them as offline instead of unknown.
     */
    public BackendServer findByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (BackendServer server : allEntries()) {
            if (server.getName() != null && server.getName().equalsIgnoreCase(name)) {
                return server;
            }
        }
        return null;
    }

    /**
     * The display names of EVERY configured server, for the {@code /server} list. Unusable entries
     * are listed too (they simply show up as offline when a player tries to join them).
     */
    public List<String> serverNames() {
        return allEntries().stream()
                .map(BackendServer::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<BackendServer> allEntries() {
        List<BackendServer> servers = properties.getServerListConfig().getServerList();
        return servers == null ? List.of() : servers;
    }

    /**
     * Every configured server entry (including ones without a usable host/port), for status and
     * player queries. Usable entries are the same list {@link #select(String)} routes from.
     */
    public List<BackendServer> listServers() {
        return allEntries();
    }

    /**
     * Whether the backend currently answers a TCP connection. Results are cached for a few seconds
     * so repeated {@code /server} checks do not re-probe. Servers without a usable host/port are
     * always reported offline. Blocks (probe timeout) — use {@link #isOnlineAsync(BackendServer)}
     * from event-loop threads.
     */
    public boolean isOnline(BackendServer server) {
        if (server == null || server.getHost() == null || server.getHost().isBlank() || server.getPort() <= 0) {
            return false;
        }
        String key = server.getHost().toLowerCase(Locale.ROOT) + ":" + server.getPort();
        OnlineStatus cached = onlineCache.get(key);
        if (cached != null && System.currentTimeMillis() - cached.checkedAt() < ONLINE_CACHE_MILLIS) {
            return cached.online();
        }

        boolean online = probe(server.getHost(), server.getPort());
        onlineCache.put(key, new OnlineStatus(online, System.currentTimeMillis()));
        log.debug("Probed backend {}:{} -> online={}", server.getHost(), server.getPort(), online);
        return online;
    }

    /** Asynchronous variant of {@link #isOnline(BackendServer)} for Netty event-loop threads. */
    public CompletableFuture<Boolean> isOnlineAsync(BackendServer server) {
        return CompletableFuture.supplyAsync(() -> isOnline(server), probeExecutor);
    }

    private static boolean probe(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private record OnlineStatus(boolean online, long checkedAt) {
    }
}
