package moe.koseirin.nyanruaineo.network.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.ProtocolVersion;
import moe.koseirin.nyanruaineo.network.Minecraft.util.FileUtils;
import moe.koseirin.nyanruaineo.network.Minecraft.util.PlaceholderResolver;
import moe.koseirin.nyanruaineo.network.Minecraft.util.RGBColorConverter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PingResponseProvider {

    private final ProxyProperties properties;
    private final ObjectMapper objectMapper;
    private final PlaceholderResolver placeholderResolver;

    // 图标缓存，支持动态更新（可通过事件监听重新加载）
    private final AtomicReference<String> faviconBase64 = new AtomicReference<>("");
    private final Random random = new Random();

    private static final String ICON_PATH = "Data/server-icon.png";
    private static final String FAVICON_PREFIX = "data:image/png;base64,";

    @PostConstruct
    public void loadIcon() {
        reloadIcon();
    }

    public void reloadIcon() {
        try {
            File iconFile = new File(ICON_PATH);
            if (iconFile.exists() && iconFile.isFile()) {
                byte[] bytes = FileUtils.readFileToByteArray(iconFile);
                String base64 = Base64.getEncoder().encodeToString(bytes);
                faviconBase64.set(FAVICON_PREFIX + base64);
                log.info("Loaded server icon from {}", ICON_PATH);
            } else {
                faviconBase64.set("");
                log.warn("Server icon not found at {}", ICON_PATH);
            }
        } catch (Exception e) {
            log.error("Failed to load server icon", e);
            faviconBase64.set("");
        }
    }

    /**
     * Builds the server list ping JSON. The reported protocol version echoes the client's own
     * protocol number so every supported client version sees the proxy as compatible, without a
     * hardcoded version in the MOTD configuration. {@code realOnline} is the number of players
     * currently in the play phase.
     */
    public String getPingJson(int clientProtocol, int realOnline) {
        JSONObject root = new JSONObject();

        // 获取 MOTD 配置
        ProxyProperties.MotdConfig motdConfig = properties.getMotdConfig();

        // 更新真实在线人数
        placeholderResolver.updateRealOnline(realOnline);

        // 选择 MOTD 条目（随机或固定）
        List<String> motdLines;
        List<String> hoverLines;

        if (motdConfig.getRandomMotds() != null && !motdConfig.getRandomMotds().isEmpty()) {
            int totalWeight = motdConfig.getRandomMotds().stream()
                    .mapToInt(ProxyProperties.MotdEntry::getWeight).sum();
            int r = random.nextInt(totalWeight);
            int cumulative = 0;
            ProxyProperties.MotdEntry selected = null;
            for (ProxyProperties.MotdEntry entry : motdConfig.getRandomMotds()) {
                cumulative += entry.getWeight();
                if (r < cumulative) {
                    selected = entry;
                    break;
                }
            }
            if (selected != null) {
                motdLines = selected.getLines();
                hoverLines = selected.getHoverLines();
            } else {
                motdLines = motdConfig.getLines();
                hoverLines = motdConfig.getHoverLines();
            }
        } else {
            motdLines = motdConfig.getLines();
            hoverLines = motdConfig.getHoverLines();
        }

        int protocol = clientProtocol;
        String firstLine = motdLines.isEmpty() ? "" : motdLines.get(0);
        String secondLine = motdLines.size() > 1 ? motdLines.get(1) : "";

        firstLine = placeholderResolver.resolve(firstLine, motdConfig);
        secondLine = placeholderResolver.resolve(secondLine, motdConfig);

        firstLine = RGBColorConverter.convert(firstLine, protocol);
        secondLine = RGBColorConverter.convert(secondLine, protocol);

        // description
        JSONObject description = new JSONObject();
        description.put("text", firstLine + "\n" + secondLine);
        root.put("description", description);

        // version
        JSONObject version = new JSONObject();
        ProtocolVersion knownVersion = ProtocolVersion.fromProtocol(clientProtocol);
        String versionName = knownVersion == ProtocolVersion.UNKNOWN
                ? "Minecraft " + clientProtocol
                : knownVersion.getVersionName();
        version.put("name", versionName);
        version.put("protocol", clientProtocol);
        root.put("version", version);

        // players
        JSONObject players = new JSONObject();
        int maxPlayers = motdConfig.getMaxPlayers();
        if (maxPlayers <= 0) {
            maxPlayers = 100;
        }
        players.put("max", maxPlayers);

        int onlinePlayers;
        if (motdConfig.isFakePlayersEnabled()) {
            String totalOnlineStr = placeholderResolver.resolve("%total_online%", motdConfig);
            onlinePlayers = Integer.parseInt(totalOnlineStr);
        } else {
            onlinePlayers = realOnline;
        }
        players.put("online", onlinePlayers);

        if (hoverLines != null && !hoverLines.isEmpty()) {
            JSONArray sample = new JSONArray();
            for (String line : hoverLines) {
                line = placeholderResolver.resolve(line, motdConfig);
                line = RGBColorConverter.convert(line, protocol);
                JSONObject player = new JSONObject();
                player.put("name", line);
                player.put("id", "00000000-0000-0000-0000-000000000000");
                sample.add(player);
            }
            players.put("sample", sample);
        }
        root.put("players", players);

        // favicon
        String icon = faviconBase64.get();
        if (!icon.isEmpty()) {
            root.put("favicon", icon);
        }

        // ----------modinfo字段 ----------
        JSONObject modinfo = new JSONObject();
        modinfo.put("type", "FML");
        JSONArray modList = new JSONArray();
        JSONObject mod = new JSONObject(); mod.put("modid", "examplemod"); mod.put("version", "1.0.0"); modList.add(mod);
        modinfo.put("modList", modList);
        root.put("modinfo", modinfo);
        // -----------------------------------------

        try {
            return JSONObject.toJSONString(root);
        } catch (Exception e) {
            log.error("Failed to serialize ping response", e);
            return "{\"description\":{\"text\":\"Ping error\"}}";
        }
    }

    @Data
    private static class PingResponse {
        private Version version = new Version();
        private Players players = new Players();
        private Description description = new Description();
        private String favicon;

        @Data
        private static class Version {
            private String name;
            private int protocol;
        }

        @Data
        private static class Players {
            private int max;
            private int online;
            private List<Object> sample;
        }

        @Data
        private static class Description {
            private String text;
        }
    }
}
