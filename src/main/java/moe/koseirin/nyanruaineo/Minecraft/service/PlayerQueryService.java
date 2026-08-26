package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.RequiredArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Query service for the proxy's online players: who is online, where they are connected, and the
 * per-server player counts used by the status views.
 */
@Component
@RequiredArgsConstructor
public class PlayerQueryService {

    private final MinecraftProxy proxy;
    private final BackendServerManager backendServerManager;

    /** Snapshot of one online player. */
    public record PlayerInfo(String username, UUID uuid, int protocolVersion,
                             String serverName, String serverHost, int serverPort) {
    }

    /** Every player currently in the play phase. */
    public List<PlayerInfo> getOnlinePlayers() {
        List<PlayerInfo> players = new ArrayList<>();
        for (UserConnection user : proxy.getOnlineUsers()) {
            ServerConnection server = user.getServer();
            players.add(new PlayerInfo(
                    user.getUsername(),
                    user.getUuid(),
                    user.getProtocolVersion(),
                    server == null ? null : resolveServerName(server),
                    server == null ? null : server.getHost(),
                    server == null ? -1 : server.getPort()));
        }
        return players;
    }

    /** Looks up an online player by name (case-insensitive). */
    public PlayerInfo findPlayer(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (PlayerInfo player : getOnlinePlayers()) {
            if (player.username() != null && player.username().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    /** 模糊匹配版本awa. */
    public List<PlayerInfo> findPlayers(String name) {
        if (name == null || name.isBlank()) {
            return Collections.emptyList();
        }

        String lowerName = name.toLowerCase();
        List<PlayerInfo> online = getOnlinePlayers();

        List<PlayerInfo> exact = new ArrayList<>();
        List<PlayerInfo> prefix = new ArrayList<>();
        List<PlayerInfo> contains = new ArrayList<>();

        for (PlayerInfo player : online) {
            String username = player.username();
            if (username == null) continue;
            String lowerUser = username.toLowerCase();

            if (lowerUser.equals(lowerName)) {
                exact.add(player);
            } else if (lowerUser.startsWith(lowerName)) {
                prefix.add(player);
            } else if (lowerUser.contains(lowerName)) {
                contains.add(player);
            }
        }

        List<PlayerInfo> result = new ArrayList<>();
        result.addAll(exact);
        result.addAll(prefix);
        result.addAll(contains);
        return result;
    }

    /** The live {@link UserConnection} of an online player, or {@code null}. */
    public UserConnection getUserConnection(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (UserConnection user : proxy.getOnlineUsers()) {
            if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(name)
                    && user.getChannel() != null && user.getChannel().isActive()) {
                return user;
            }
        }
        return null;
    }

    public boolean isOnline(String name) {
        return getUserConnection(name) != null;
    }

    public int getOnlineCount() {
        return proxy.getOnlineCount();
    }

    /** Number of online players connected to the given backend address. */
    public int countPlayersOn(String host, int port) {
        int count = 0;
        for (UserConnection user : proxy.getOnlineUsers()) {
            ServerConnection server = user.getServer();
            if (server != null && host != null && host.equalsIgnoreCase(server.getHost())
                    && port == server.getPort()) {
                count++;
            }
        }
        return count;
    }

    /** Resolves a backend connection to its configured server name, falling back to host:port. */
    private String resolveServerName(ServerConnection server) {
        for (BackendServer backend : backendServerManager.listServers()) {
            if (backend.getHost() != null && backend.getHost().equalsIgnoreCase(server.getHost())
                    && backend.getPort() == server.getPort()) {
                return backend.getName();
            }
        }
        return server.getHost() + ":" + server.getPort();
    }
}
