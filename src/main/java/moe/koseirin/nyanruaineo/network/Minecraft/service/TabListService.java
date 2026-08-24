package moe.koseirin.nyanruaineo.network.Minecraft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerInfoRemove;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerInfoUpdate;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerListItem;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.TabListHeaderFooter;
import moe.koseirin.nyanruaineo.network.Minecraft.util.ChatComponentUtils;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepts and modifies the clientbound TabList packets, driven by the {@code proxy.tablist}
 * database config: replaces the header/footer text (1.8-1.20.2) and wraps every player name in
 * the configured prefix/suffix (1.8-1.18.2). Supports the placeholders {@code %online%},
 * {@code %max%} (header/footer) and {@code %server%} — the current sub-server: per entry it is
 * the server that entry's player is on (resolved by UUID, falling back to the viewer's server for
 * entries that are not proxied players). When disabled every packet passes through untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TabListService {

    /** How long the cached config is reused before the next read of the in-memory cache. */
    private static final long CONFIG_CACHE_MILLIS = 5000;

    private final ProxyProperties properties;
    private final BackendServerManager backendServerManager;

    private volatile ProxyProperties.TabListConfig cachedConfig;
    private volatile long cachedConfigAt;

    private volatile List<BackendServer> cachedServers;
    private volatile long cachedServersAt;

    /**
     * The TabList config with a short in-memory cache of the parsed object. The underlying read
     * hits the {@link SystemConfigCacheService} map (loaded at startup, refreshed manually via
     * ReloadConfig or by the config edit flow), never the database.
     */
    private ProxyProperties.TabListConfig config() {
        long now = System.currentTimeMillis();
        ProxyProperties.TabListConfig config = cachedConfig;
        if (config == null || now - cachedConfigAt > CONFIG_CACHE_MILLIS) {
            config = properties.getTabListConfig();
            cachedConfig = config;
            cachedConfigAt = now;
        }
        return config;
    }

    /** The configured server list with the same short cache (resolved per TabList entry). */
    private List<BackendServer> servers() {
        long now = System.currentTimeMillis();
        List<BackendServer> list = cachedServers;
        if (list == null || now - cachedServersAt > CONFIG_CACHE_MILLIS) {
            list = backendServerManager.listServers();
            cachedServers = list;
            cachedServersAt = now;
        }
        return list;
    }

    /**
     * Builds the configured header/footer packet for the proxy to push to a client when it enters
     * the play phase (join or server switch), mirroring BungeeCord's {@code setTabHeader} — the
     * proxy owns the TabList header/footer instead of waiting for the backend to send one. Returns
     * {@code null} when the feature is disabled, nothing is configured, or the version encodes
     * components as NBT (1.20.3+, not supported).
     */
    public TabListHeaderFooter buildHeaderFooter(UserConnection user, int onlineCount) {
        if (user.getProtocolVersion() >= 765) {
            return null;
        }
        ProxyProperties.TabListConfig config = config();
        if (!config.isEnabled()) {
            return null;
        }
        String header = null;
        String footer = null;
        String serverName = serverNameOf(user);
        if (config.getHeader() != null && !config.getHeader().isEmpty()) {
            header = buildLines(config.getHeader(), onlineCount, serverName);
        }
        if (config.getFooter() != null && !config.getFooter().isEmpty()) {
            footer = buildLines(config.getFooter(), onlineCount, serverName);
        }
        if (header == null && footer == null) {
            return null;
        }
        return new TabListHeaderFooter(header, footer);
    }

    /**
     * Replaces the header/footer components of the packet with the configured lines. The packet is
     * only touched when the feature is enabled and lines are configured; otherwise the backend's
     * own header/footer reaches the client unchanged.
     */
    public void applyHeaderFooter(TabListHeaderFooter packet, UserConnection user, int onlineCount) {
        ProxyProperties.TabListConfig config = config();
        if (!config.isEnabled()) {
            return;
        }
        String serverName = serverNameOf(user);
        if (config.getHeader() != null && !config.getHeader().isEmpty()) {
            packet.setHeader(buildLines(config.getHeader(), onlineCount, serverName));
        }
        if (config.getFooter() != null && !config.getFooter().isEmpty()) {
            packet.setFooter(buildLines(config.getFooter(), onlineCount, serverName));
        }
    }

    /**
     * Rewrites the display name of every added/renamed entry as {@code prefix + name + suffix},
     * resolving {@code %server%} per entry (the entry player's server, or the viewer's server when
     * the entry does not belong to a proxied player). Names of {@code UPDATE_DISPLAY_NAME} entries
     * are resolved from the names seen in previous {@code ADD_PLAYER} packets (tracked per player
     * on the {@link UserConnection}).
     */
    public void decorate(PlayerListItem packet, UserConnection user, Collection<UserConnection> onlineUsers) {
        ProxyProperties.TabListConfig config = config();
        if (!config.isEnabled()) {
            return;
        }
        String prefix = config.getPrefix() == null ? "" : config.getPrefix();
        String suffix = config.getSuffix() == null ? "" : config.getSuffix();
        if (prefix.isEmpty() && suffix.isEmpty()) {
            return;
        }

        String fallbackServer = serverNameOf(user);
        Map<UUID, String> names = user.getTabListNames();
        for (PlayerListItem.Item item : packet.getItems()) {
            switch (packet.getAction()) {
                case PlayerListItem.ACTION_ADD_PLAYER:
                    if (item.getUsername() != null) {
                        names.put(item.getUuid(), item.getUsername());
                    }
                    decorate(item, names, prefix, suffix, fallbackServer, onlineUsers);
                    break;
                case PlayerListItem.ACTION_UPDATE_DISPLAY_NAME:
                    decorate(item, names, prefix, suffix, fallbackServer, onlineUsers);
                    break;
                case PlayerListItem.ACTION_REMOVE_PLAYER:
                    names.remove(item.getUuid());
                    break;
                default:
                    break;
            }
        }
    }

    private void decorate(PlayerListItem.Item item, Map<UUID, String> names,
                          String prefix, String suffix, String fallbackServer,
                          Collection<UserConnection> onlineUsers) {
        String name = item.getUsername() != null ? item.getUsername() : names.get(item.getUuid());
        if (name == null) {
            return;
        }
        String serverName = serverNameOf(item.getUuid(), onlineUsers);
        if (serverName == null) {
            serverName = fallbackServer;
        }
        if (serverName == null) {
            serverName = "?";
        }

        String resolvedPrefix = prefix.replace("%server%", serverName);
        String resolvedSuffix = suffix.replace("%server%", serverName);
        item.setDisplayName(ChatComponentUtils.component(resolvedPrefix + name + resolvedSuffix).toJSONString());
    }

    /**
     * 1.19.3+ variant of {@link #decorate(PlayerListItem, UserConnection, Collection)}: rewrites
     * the display names of a {@link PlayerInfoUpdate} packet and adds the
     * {@code UPDATE_DISPLAY_NAME} action bit when names were decorated but the packet did not
     * carry display names yet (the bit applies to every entry of the packet).
     */
    public void decorateUpdate(PlayerInfoUpdate packet, UserConnection user, Collection<UserConnection> onlineUsers) {
        ProxyProperties.TabListConfig config = config();
        if (!config.isEnabled()) {
            return;
        }
        String prefix = config.getPrefix() == null ? "" : config.getPrefix();
        String suffix = config.getSuffix() == null ? "" : config.getSuffix();
        if (prefix.isEmpty() && suffix.isEmpty()) {
            return;
        }

        String fallbackServer = serverNameOf(user);
        Map<UUID, String> names = user.getTabListNames();
        boolean decoratedAny = false;
        for (PlayerInfoUpdate.Item item : packet.getItems()) {
            boolean hasAdd = packet.hasAction(PlayerInfoUpdate.ACTION_ADD_PLAYER);
            boolean hasDisplayName = packet.hasAction(PlayerInfoUpdate.ACTION_UPDATE_DISPLAY_NAME);
            if (hasAdd && item.getUsername() != null) {
                names.put(item.getUuid(), item.getUsername());
            }
            if (!hasAdd && !hasDisplayName) {
                continue;
            }
            String name = item.getUsername() != null ? item.getUsername() : names.get(item.getUuid());
            if (name == null) {
                continue;
            }
            String serverName = serverNameOf(item.getUuid(), onlineUsers);
            if (serverName == null) {
                serverName = fallbackServer;
            }
            if (serverName == null) {
                serverName = "?";
            }
            String resolvedPrefix = prefix.replace("%server%", serverName);
            String resolvedSuffix = suffix.replace("%server%", serverName);
            item.setDisplayName(ChatComponentUtils.component(resolvedPrefix + name + resolvedSuffix).toJSONString());
            decoratedAny = true;
        }
        if (decoratedAny && !packet.hasAction(PlayerInfoUpdate.ACTION_UPDATE_DISPLAY_NAME)) {
            packet.setActions(packet.getActions() | (1 << PlayerInfoUpdate.ACTION_UPDATE_DISPLAY_NAME));
        }
    }

    /** Forgets the TabList names of the players removed by a 1.19.3+ remove packet. */
    public void removeEntries(PlayerInfoRemove packet, UserConnection user) {
        Map<UUID, String> names = user.getTabListNames();
        for (UUID uuid : packet.getUuids()) {
            names.remove(uuid);
        }
    }

    /**
     * BungeeCord {@code TabListHandler.onServerChange} parity: when the player switches servers,
     * removal packets are sent for every entry the old server added, so the client's TabList can
     * never carry stale entries into the new world. (1.19-1.19.2 player info packets are not
     * registered, so those versions rely on the JoinGame reset instead.)
     */
    public void resetTabList(UserConnection user) {
        Map<UUID, String> names = user.getTabListNames();
        if (names.isEmpty()) {
            return;
        }
        int version = user.getProtocolVersion();
        List<UUID> uuids = new ArrayList<>(names.keySet());
        if (version < 759) {
            List<PlayerListItem.Item> items = new ArrayList<>(uuids.size());
            for (UUID uuid : uuids) {
                PlayerListItem.Item item = new PlayerListItem.Item();
                item.setUuid(uuid);
                items.add(item);
            }
            user.sendPacket(new PlayerListItem(PlayerListItem.ACTION_REMOVE_PLAYER, items));
        } else if (version >= 761 && version <= 764) {
            user.sendPacket(new PlayerInfoRemove(uuids));
        }
        names.clear();
    }

    private String buildLines(List<String> lines, int onlineCount, String serverName) {
        String joined = String.join("\n", lines);
        joined = joined.replace("%online%", String.valueOf(onlineCount));
        joined = joined.replace("%max%", String.valueOf(properties.getMaxPlayers()));
        if (serverName != null) {
            joined = joined.replace("%server%", serverName);
        }
        return ChatComponentUtils.component(joined).toJSONString();
    }

    /** The configured server name of the connection, or {@code host:port} when not configured. */
    private String serverNameOf(UserConnection user) {
        ServerConnection server = user.getServer();
        if (server == null) {
            return "?";
        }
        for (BackendServer backend : servers()) {
            if (backend.getHost() != null && backend.getHost().equalsIgnoreCase(server.getHost())
                    && backend.getPort() == server.getPort()) {
                return backend.getName();
            }
        }
        return server.getHost() + ":" + server.getPort();
    }

    /** The server name a UUID's player is currently on, or {@code null} when not a proxied player. */
    private String serverNameOf(UUID uuid, Collection<UserConnection> onlineUsers) {
        for (UserConnection user : onlineUsers) {
            if (uuid.equals(user.getUuid())) {
                if (user.getServer() == null) {
                    return null;
                }
                return serverNameOf(user);
            }
        }
        return null;
    }
}
