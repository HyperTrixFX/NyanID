package moe.koseirin.nyanruaineo.network.Minecraft.config;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/*
 * @author KoseiRin_
 * awa
 */
@Component
public class ProxyProperties {
    private static final String KEY_PORT = "proxy.port";
    private static final String KEY_BACKEND_HOST = "proxy.backend.host";
    private static final String KEY_BACKEND_PORT = "proxy.backend.port";
    private static final String KEY_MOTD = "proxy.motd";
    private static final String KEY_MAX_PLAYERS = "proxy.maxPlayers";
    private static final String KEY_ONLINE_MODE = "proxy.online-mode";

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

    public String getBackendHost() {
        String val = cacheService.getConfig(KEY_BACKEND_HOST);
        if (val == null){
            cacheService.addConfig(KEY_BACKEND_HOST,"localhost");
            return "localhost";
        }
        return val;
    }

    public int getBackendPort() {
        String val = cacheService.getConfig(KEY_BACKEND_PORT);
        if (val == null){
            cacheService.addConfig(KEY_BACKEND_PORT,"25566");
            return 25566;
        }
        return Integer.parseInt(val);
    }
    public MotdConfig getMotdConfig() {
        String val = cacheService.getConfig(KEY_MOTD);
        if (val == null) {
            MotdConfig defaultConfig = new MotdConfig();
            defaultConfig.setLines(Arrays.asList(
                    "§a§lMinecraft Proxy",
                    "§7Powered by Spring Boot"
            ));
            defaultConfig.setFakePlayersEnabled(true);
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
    public boolean isOnlineMode() {
        String val = cacheService.getConfig(KEY_ONLINE_MODE);
        if (val == null){
            cacheService.addConfig(KEY_ONLINE_MODE,"true");
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
}
