package moe.koseirin.nyanruaineo.network.Minecraft.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.service.BackendServer;
import moe.koseirin.nyanruaineo.network.Minecraft.service.ServerListConfig;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/*
 * @author KoseiRin_
 * awa
 */
@Slf4j
@Component
public class ProxyProperties {
    private static final String KEY_PORT = "proxy.port";
    private static final String KEY_BACKEND_SERVERS = "proxy.backend.servers";
    private static final String KEY_MOTD = "proxy.motd";
    private static final String KEY_TABLIST = "proxy.tablist";
    private static final String KEY_KICK_MESSAGE = "proxy.kick-message";
    private static final String KEY_MAX_PLAYERS = "proxy.maxPlayers";
    private static final String KEY_ONLINE_MODE = "proxy.online-mode";
    private static final String KEY_IP_FORWARD = "proxy.ip-forward";

    private final SystemConfigCacheService cacheService;

    public ProxyProperties(SystemConfigCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public int getPort() {
        String val = cacheService.getConfig(KEY_PORT);
        if (val == null){
            cacheService.addConfig(KEY_PORT,"25565");
            return 25565;
        }
        return Integer.parseInt(val);
    }

    /**
     * Reads the sub-server list from {@code proxy.backend.servers}. The stored value is a
     * {@link ServerListConfig} object:
     *
     * <pre>
     * {
     *   "default_server": "lobby",
     *   "server_list": [
     *     { "uid": "lobby-001", "priority": 1, "name": "lobby", "host": "localhost", "port": 25566 },
     *     { "uid": "survival-001", "priority": 2, "name": "survival", "host": "localhost", "port": 25567 }
     *   ]
     * }
     * </pre>
     *
     * The value is read from the in-memory {@link SystemConfigCacheService} (loaded at startup,
     * kept up to date by the config edit flow and the manual {@code ReloadConfig} refresh — the
     * database is never queried here). A missing key writes the default config back; the legacy
     * JSON-array format is migrated automatically. The old single-backend
     * {@code proxy.backend.host/port} keys are no longer used.
     */
    public ServerListConfig getServerListConfig() {
        String val = cacheService.getConfig(KEY_BACKEND_SERVERS);

        if (val == null || val.isBlank()) {
            ServerListConfig defaultConfig = new ServerListConfig();
            defaultConfig.setDefaultServer("lobby");
            List<BackendServer> servers = new ArrayList<>();
            servers.add(new BackendServer("lobby-001", 1, "lobby", "localhost", 25566));
            servers.add(new BackendServer("survival-001", 2, "survival", "localhost", 25567));
            defaultConfig.setServerList(servers);
            try {
                cacheService.updateConfig(KEY_BACKEND_SERVERS, JSON.toJSONString(defaultConfig));
            } catch (Exception e) {
                try {
                    cacheService.addConfig(KEY_BACKEND_SERVERS, JSON.toJSONString(defaultConfig));
                } catch (Exception ignored) {
                    // Already present or no transaction — the default is returned regardless.
                }
            }
            return defaultConfig;
        }

        try {
            String trimmed = val.trim();
            if (trimmed.startsWith("[")) {
                // Legacy JSON-array format: migrate to the new object format.
                List<BackendServer> list = JSON.parseArray(trimmed, BackendServer.class);
                ServerListConfig migrated = new ServerListConfig();
                migrated.setServerList(list == null ? new ArrayList<>() : list);
                List<BackendServer> migratedList = migrated.getServerList();
                if (migratedList.isEmpty()) {
                    migrated.setDefaultServer(null);
                } else {
                    migratedList.sort(Comparator.comparingInt(BackendServer::getPriority));
                    migrated.setDefaultServer(migratedList.get(0).getName());
                }
                try {
                    cacheService.updateConfig(KEY_BACKEND_SERVERS, JSON.toJSONString(migrated));
                } catch (Exception ignored) {
                }
                log.info("Migrated {} to the new server list format: {}", KEY_BACKEND_SERVERS, migrated);
                return migrated;
            }
            ServerListConfig config = JSON.parseObject(trimmed, ServerListConfig.class);
            if (config != null && config.getServerList() == null) {
                // Some editors store the object with camelCase keys; accept them as aliases so the
                // server list is never silently lost.
                JSONObject raw = JSON.parseObject(trimmed);
                if (raw != null) {
                    List<BackendServer> list = raw.getList("serverList", BackendServer.class);
                    if (list == null) {
                        list = raw.getList("server_list", BackendServer.class);
                    }
                    if (list != null) {
                        config.setServerList(list);
                    }
                    String def = raw.getString("defaultServer");
                    if (def == null) {
                        def = raw.getString("default_server");
                    }
                    config.setDefaultServer(def);
                }
            }
            if (config == null) {
                log.error("Parsed {} to null; the server list will be empty (value: {})", KEY_BACKEND_SERVERS, val);
                return new ServerListConfig();
            }
            if (config.getServerList() == null) {
                log.warn("{} contains no server_list; the /server list will be empty (value: {})",
                        KEY_BACKEND_SERVERS, val);
                config.setServerList(new ArrayList<>());
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to parse {} (value: {}): {}", KEY_BACKEND_SERVERS, val, e.getMessage());
            return new ServerListConfig();
        }
    }

    /**
     * The configured sub-server list. Entries without a concrete host/port are skipped with a
     * warning so a misconfigured entry cannot break the whole list.
     */
    public List<BackendServer> getBackendServers() {
        ServerListConfig config = getServerListConfig();
        List<BackendServer> servers = config.getServerList();
        if (servers == null) {
            return List.of();
        }
        List<BackendServer> valid = new ArrayList<>();
        for (BackendServer server : servers) {
            if (server.getHost() == null || server.getHost().isBlank() || server.getPort() <= 0) {
                log.warn("Skipping server entry with missing host/port: {}", server);
                continue;
            }
            valid.add(server);
        }
        return valid;
    }
    public MotdConfig getMotdConfig() {
        String val = cacheService.getConfig(KEY_MOTD);
        if (val == null) {
            MotdConfig defaultConfig = new MotdConfig();
            defaultConfig.setLines(Arrays.asList(
                    "§a§lMinecraft Proxy",
                    "§7Powered by Spring Boot"
            ));
            defaultConfig.setFakePlayersEnabled(false);
            defaultConfig.setFakePlayersMin(10);
            defaultConfig.setFakePlayersMax(50);
            defaultConfig.setFakePlayersIncrement(1);
            defaultConfig.setMaxPlayers(100);
            defaultConfig.setVersionName("Minecraft 1.20.1");
            defaultConfig.setProtocolVersion(763);
            defaultConfig.setHoverLines(Arrays.asList(
                    "§6Welcome to our proxy!",
                    "§7Online: §a%online%",
                    "§7Fake: §e%fake_online%"
            ));

            String json = JSON.toJSONString(defaultConfig);
            cacheService.addConfig(KEY_MOTD, json);
            return defaultConfig;
        }

        return JSON.parseObject(val, MotdConfig.class);
    }

    public int getMaxPlayers() {
        String val = cacheService.getConfig(KEY_MAX_PLAYERS);
        if (val == null){
            cacheService.addConfig(KEY_MAX_PLAYERS,"100");
            return 100;
        }
        return Integer.parseInt(val);
    }

    /**
     * Reads the TabList interception config from {@code proxy.tablist}:
     *
     * <pre>
     * {
     *   "enabled": true,
     *   "header": ["§6§lNyanID 服务器群", "§7在线: §a%online%"],
     *   "footer": ["§7跨服代理 · §b/server 切换"],
     *   "prefix": "§7[§a玩家§7] §f",
     *   "suffix": ""
     * }
     * </pre>
     *
     * The value is read fresh from the database on every use so runtime edits take effect without
     * a restart; a missing key writes the (disabled) default back to the cache. Header/footer
     * lines support {@code %online%} / {@code %max%} placeholders; prefix/suffix wrap every player
     * name shown in the TabList (1.8-1.18.2 clients).
     */
    public TabListConfig getTabListConfig() {
        String val = cacheService.getConfig(KEY_TABLIST);

        if (val == null || val.isBlank()) {
            TabListConfig defaultConfig = new TabListConfig();
            defaultConfig.setEnabled(false);
            defaultConfig.setHeader(Arrays.asList("§6§lNyanID 服务器群", "§7在线: §a%online%"));
            defaultConfig.setFooter(Arrays.asList("§7跨服代理 · §b/server 切换"));
            defaultConfig.setPrefix("");
            defaultConfig.setSuffix(" §8[%server%]");
            try {
                cacheService.updateConfig(KEY_TABLIST, JSON.toJSONString(defaultConfig));
            } catch (Exception e) {
                try {
                    cacheService.addConfig(KEY_TABLIST, JSON.toJSONString(defaultConfig));
                } catch (Exception ignored) {
                    // Already present or no transaction — the default is returned regardless.
                }
            }
            return defaultConfig;
        }

        try {
            TabListConfig config = JSON.parseObject(val.trim(), TabListConfig.class);
            if (config == null) {
                log.error("Parsed {} to null; TabList interception disabled (value: {})", KEY_TABLIST, val);
                return new TabListConfig();
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to parse {} (value: {}): {}", KEY_TABLIST, val, e.getMessage());
            return new TabListConfig();
        }
    }

    /**
     * Reads the kick message config from {@code proxy.kick-message}:
     *
     * <pre>
     * {
     *   "enabled": true,
     *   "banned_message_base": "&5&l緒山まひろ ...\n&b&l»&f&lPlayer: &4$playerName\n..."
     * }
     * </pre>
     *
     * The template supports {@code &} colour codes, {@code \n} (or {@code |}) line breaks and the
     * {@code $playerName} / {@code $reason} / {@code $idRandom} placeholders. Read fresh from the
     * database on every use; a missing key writes the default back to the cache.
     */
    public KickMessageConfig getKickMessageConfig() {
        String val = cacheService.getConfig(KEY_KICK_MESSAGE);

        if (val == null || val.isBlank()) {
            KickMessageConfig defaultConfig = new KickMessageConfig();
            defaultConfig.setEnabled(true);
            defaultConfig.setBannedMessageBase(
                    "&5&l緒山まひろ &b&l» &5&l呐呐~杂鱼哥哥不会这样就被&4&lBAN&5&l的不会说话了吧♡真是弱哎&5&l♡~ &f\n"
                            + "&b&l»&f&lPlayer: &4$playerName\n"
                            + "&b&l»&f&lReason: &c&l$reason&f&3&l\n"
                            + "&b&l»&f&lBanID : &c&l$idRandom\n"
                            + "&5&lFind out more:&b&l»&f&l http://www.nyacat.cloud &9");
            try {
                cacheService.updateConfig(KEY_KICK_MESSAGE, JSON.toJSONString(defaultConfig));
            } catch (Exception e) {
                try {
                    cacheService.addConfig(KEY_KICK_MESSAGE, JSON.toJSONString(defaultConfig));
                } catch (Exception ignored) {
                    // Already present or no transaction — the default is returned regardless.
                }
            }
            return defaultConfig;
        }

        try {
            KickMessageConfig config = JSON.parseObject(val.trim(), KickMessageConfig.class);
            if (config == null) {
                log.error("Parsed {} to null; using the fallback kick message (value: {})", KEY_KICK_MESSAGE, val);
                return new KickMessageConfig();
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to parse {} (value: {}): {}", KEY_KICK_MESSAGE, val, e.getMessage());
            return new KickMessageConfig();
        }
    }

    public boolean isOnlineMode() {
        String val = cacheService.getConfig(KEY_ONLINE_MODE);
        if (val == null){
            cacheService.addConfig(KEY_ONLINE_MODE,"true");
            return true;
        }
        return Boolean.parseBoolean(val);
    }

    /**
     * Whether to forward the player's IP, UUID and skin properties to the backend via BungeeCord
     * IP forwarding. Required when the backend runs with {@code bungeecord: true} in spigot.yml.
     */
    public boolean isIpForward() {
        String val = cacheService.getConfig(KEY_IP_FORWARD);
        if (val == null){
            cacheService.addConfig(KEY_IP_FORWARD,"true");
            return true;
        }
        return Boolean.parseBoolean(val);
    }
    @Data
    public static class MotdConfig {
        private List<String> lines;

        private List<String> hoverLines;

        private boolean fakePlayersEnabled;
        private int fakePlayersMin;
        private int fakePlayersMax;
        private int fakePlayersIncrement;

        private int maxPlayers;

        private String versionName;
        private int protocolVersion;

        private List<MotdEntry> randomMotds;

        private boolean rgbSupport;
    }

    @Data
    public static class MotdEntry {
        private List<String> lines;
        private List<String> hoverLines;
        private int weight;
    }

    /** TabList interception config stored under {@code proxy.tablist}. */
    @Data
    public static class TabListConfig {
        private boolean enabled;
        /** Header lines (legacy § codes supported, {@code %online%}/{@code %max%}/{@code %server%} placeholders). */
        private List<String> header;
        /** Footer lines (same format as the header). */
        private List<String> footer;
        /** Prepended to every player name shown in the TabList (1.8-1.18.2). {@code %server%} = the player's current sub-server. */
        private String prefix;
        /** Appended to every player name shown in the TabList (1.8-1.18.2). {@code %server%} = the player's current sub-server. */
        private String suffix;
    }

    /** Kick message template config stored under {@code proxy.kick-message}. */
    @Data
    public static class KickMessageConfig {
        private boolean enabled;
        /**
         * The kick screen template: {@code &} colour codes, {@code \n}/{@code |} line breaks and
         * {@code $playerName} / {@code $reason} / {@code $idRandom} placeholders.
         */
        @JSONField(name = "banned_message_base")
        private String bannedMessageBase;
    }
}
