package moe.koseirin.nyanruaineo.Minecraft.config;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.FirewallConfig;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.KickMessageConfig;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.MotdConfig;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.TabListConfig;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.ServerListConfig;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class ProxyProperties {
    private static final String KEY_PORT = "proxy.port";
    private static final String KEY_BACKEND_SERVERS = "proxy.backend.servers";
    private static final String KEY_MOTD = "proxy.motd";
    private static final String KEY_TABLIST = "proxy.tablist";
    private static final String KEY_FIREWALL = "proxy.firewall";
    private static final String KEY_KICK_MESSAGE = "proxy.kick-message";
    private static final String KEY_MAX_PLAYERS = "proxy.maxPlayers";
    private static final String KEY_ONLINE_MODE = "proxy.online-mode";
    private static final String KEY_IP_FORWARD = "proxy.ip-forward";
    private static final String KEY_NAME = "proxy.name";
    private static final String KEY_FORGE_SUPPORT = "proxy.forge-support";

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
     * 从 {@code proxy.backend.servers} 读取子服务器列表。存储的值是一个
     * {@link ServerListConfig} 对象：
     *
     * <pre>
     * {
     * "default_server": "lobby",
     * "server_list": [
     * { "uid": "lobby-001", "priority": 1, "name": "lobby", "host": "localhost", "port": 25566 },
     * { "uid": "survival-001", "priority": 2, "name": "survival", "host": "localhost", "port": 25567 }
     * ]
     * }
     * </pre>
     *
     * 该值从内存中的 {@link SystemConfigCacheService} 读取（在启动时加载，
     * 由配置编辑流程和手动 {@code ReloadConfig} 刷新保持最新——这里不会查询数据库）。
     * 缺失的键会写回默认配置；旧的 JSON 数组格式会自动迁移。不再使用旧的单后台
     * {@code proxy.backend.host/port} 键。
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
                    migrated.setDefaultServer(migratedList.getFirst().getName());
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
     * 配置的子服务器列表。没有具体主机/端口的条目会被跳过并显示警告，
     * 因此错误配置的条目不会破坏整个列表。
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
     * 从 {@code proxy.tablist} 读取 TabList 拦截配置：
     *
     * <pre>
     * {
     * "enabled": true,
     * "header": ["§6§lNyanID 服务器群", "§7在线: §a%online%"],
     * "footer": ["§7跨服代理 · §b/server 切换"],
     * "prefix": "§7[§a玩家§7] §f",
     * "suffix": ""
     * }
     * </pre>
     *
     * 运行时修改配置后刷新缓存服务即可热重载；
     * 缺失的键会将（禁用的）默认值写回缓存。头部/底部行支持 {@code %online%} / {@code %max%} 占位符；前缀/后缀会包裹 TabList 中显示的每个玩家名称（1.8-1.18.2 客户端）。
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
                    // ignored
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
     * 从 {@code proxy.firewall} 读取连接防火墙配置：
     *
     * <pre>
     * {
     * "enabled": true,
     * "maxConnectionsPerSecond": 3,
     * "maxConcurrentPerIp": 5,
     * "maxLoginAttemptsPerMinute": 10,
     * "maxLoginFailuresPerMinute": 5,
     * "banDurationSeconds": 300
     * }
     * </pre>
     *
     * 缺失时写回默认值；运行时修改配置后刷新缓存即可热重载。
     */
    public FirewallConfig getFirewallConfig() {
        String val = cacheService.getConfig(KEY_FIREWALL);

        if (val == null || val.isBlank()) {
            FirewallConfig defaultConfig = new FirewallConfig();
            defaultConfig.setEnabled(true);
            defaultConfig.setMaxConnectionsPerSecond(5);
            defaultConfig.setMaxConcurrentPerIp(12);
            defaultConfig.setMaxLoginAttemptsPerMinute(20);
            defaultConfig.setMaxLoginFailuresPerMinute(6);
            defaultConfig.setBanDurationSeconds(600);
            try {
                cacheService.updateConfig(KEY_FIREWALL, JSON.toJSONString(defaultConfig));
            } catch (Exception e) {
                try {
                    cacheService.addConfig(KEY_FIREWALL, JSON.toJSONString(defaultConfig));
                } catch (Exception ignored) {
                    // ignored
                }
            }
            return defaultConfig;
        }

        try {
            FirewallConfig config = JSON.parseObject(val.trim(), FirewallConfig.class);
            if (config == null) {
                log.error("Parsed {} to null; firewall disabled (value: {})", KEY_FIREWALL, val);
                return new FirewallConfig();
            }
            return config;
        } catch (Exception e) {
            log.error("Failed to parse {} (value: {}): {}", KEY_FIREWALL, val, e.getMessage());
            return new FirewallConfig();
        }
    }

    /**
     * 从 {@code proxy.kick-message} 读取踢出消息配置：
     *
     * <pre>
     * {
     * "enabled": true,
     * "banned_message_base": "&5&l緒山まひろ ...n&b&l»&f&l玩家: &4$playerName."
     * }
     * </pre>
     *
     * 模板支持 {@code &} 颜色代码、{@code n}（或 {@code |}）换行，以及 {@code $playerName} / {@code $reason} / {@code $idRandom} 占位符。
     * 运行时修改配置后刷新缓存服务即可热重载；如果缺少某个键，则将默认值写回缓存。
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
     * 是否通过 BungeeCord 将玩家的 IP、UUID 和皮肤属性转发到后端
     * IP 转发。仅在后端的 spigot.yml 中启用 {@code bungeecord: true} 时需要。
     */
    public boolean isIpForward() {
        String val = cacheService.getConfig(KEY_IP_FORWARD);
        if (val == null){
            cacheService.addConfig(KEY_IP_FORWARD,"true");
            return true;
        }
        return Boolean.parseBoolean(val);
    }

    /**
     * 代理的显示名称，用于品牌插件消息（显示在客户端的 F3 屏幕中）
     * 以及服务器品牌重写中，类似于 BungeeCord 的 {@code bungee.name}。
     */
    public String getProxyName() {
        String val = cacheService.getConfig(KEY_NAME);
        if (val == null || val.isBlank()) {
            cacheService.addConfig(KEY_NAME, "NekoProxy");
            return "NekoProxy";
        }
        return val.trim();
    }

    /**
     * 是否启用 Forge (FML) 握手拦截，类似 BungeeCord 的{@code forge_support} 配置。
     * 启用后，代理将驱动客户端握手状态机，在服务器切换时重置它，并保护原版后端防止雷霆大数据包。
     */
    public boolean isForgeSupport() {
        String val = cacheService.getConfig(KEY_FORGE_SUPPORT);
        if (val == null) {
            cacheService.addConfig(KEY_FORGE_SUPPORT, "true");
            return true;
        }
        return Boolean.parseBoolean(val);
    }
}
