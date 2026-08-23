package moe.koseirin.nyanruaineo.network.Minecraft.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.config.ProxyProperties;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.PlayerListItem;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.TabListHeaderFooter;
import moe.koseirin.nyanruaineo.network.Minecraft.util.ChatComponentUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepts and modifies the clientbound TabList packets, driven by the {@code proxy.tablist}
 * database config: replaces the header/footer text (1.8-1.20.2) and wraps every player name in
 * the configured prefix/suffix (1.8-1.18.2). When disabled every packet passes through untouched.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TabListService {

    private final ProxyProperties properties;

    /**
     * Builds the configured header/footer packet for the proxy to push to a client when it enters
     * the play phase (join or server switch), mirroring BungeeCord's {@code setTabHeader} — the
     * proxy owns the TabList header/footer instead of waiting for the backend to send one. Returns
     * {@code null} when the feature is disabled, nothing is configured, or the version encodes
     * components as NBT (1.20.3+, not supported).
     */
    public TabListHeaderFooter buildHeaderFooter(int protocolVersion, int onlineCount) {
        if (protocolVersion >= 765) {
            return null;
        }
        ProxyProperties.TabListConfig config = properties.getTabListConfig();
        if (!config.isEnabled()) {
            return null;
        }
        String header = null;
        String footer = null;
        if (config.getHeader() != null && !config.getHeader().isEmpty()) {
            header = buildLines(config.getHeader(), onlineCount);
        }
        if (config.getFooter() != null && !config.getFooter().isEmpty()) {
            footer = buildLines(config.getFooter(), onlineCount);
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
    public void applyHeaderFooter(TabListHeaderFooter packet, int onlineCount) {
        ProxyProperties.TabListConfig config = properties.getTabListConfig();
        if (!config.isEnabled()) {
            return;
        }
        if (config.getHeader() != null && !config.getHeader().isEmpty()) {
            packet.setHeader(buildLines(config.getHeader(), onlineCount));
        }
        if (config.getFooter() != null && !config.getFooter().isEmpty()) {
            packet.setFooter(buildLines(config.getFooter(), onlineCount));
        }
    }

    /**
     * Rewrites the display name of every added/renamed entry as {@code prefix + name + suffix}.
     * Names of {@code UPDATE_DISPLAY_NAME} entries are resolved from the names seen in previous
     * {@code ADD_PLAYER} packets (tracked per player on the {@link UserConnection}).
     */
    public void decorate(PlayerListItem packet, UserConnection user) {
        ProxyProperties.TabListConfig config = properties.getTabListConfig();
        if (!config.isEnabled()) {
            return;
        }
        String prefix = config.getPrefix() == null ? "" : config.getPrefix();
        String suffix = config.getSuffix() == null ? "" : config.getSuffix();
        if (prefix.isEmpty() && suffix.isEmpty()) {
            return;
        }

        Map<UUID, String> names = user.getTabListNames();
        for (PlayerListItem.Item item : packet.getItems()) {
            switch (packet.getAction()) {
                case PlayerListItem.ACTION_ADD_PLAYER:
                    if (item.getUsername() != null) {
                        names.put(item.getUuid(), item.getUsername());
                    }
                    decorate(item, names, prefix, suffix);
                    break;
                case PlayerListItem.ACTION_UPDATE_DISPLAY_NAME:
                    decorate(item, names, prefix, suffix);
                    break;
                case PlayerListItem.ACTION_REMOVE_PLAYER:
                    names.remove(item.getUuid());
                    break;
                default:
                    break;
            }
        }
    }

    private static void decorate(PlayerListItem.Item item, Map<UUID, String> names,
                                 String prefix, String suffix) {
        String name = item.getUsername() != null ? item.getUsername() : names.get(item.getUuid());
        if (name == null) {
            return;
        }
        item.setDisplayName(ChatComponentUtils.component(prefix + name + suffix).toJSONString());
    }

    private String buildLines(List<String> lines, int onlineCount) {
        String joined = String.join("\n", lines);
        joined = joined.replace("%online%", String.valueOf(onlineCount));
        joined = joined.replace("%max%", String.valueOf(properties.getMaxPlayers()));
        return ChatComponentUtils.component(joined).toJSONString();
    }
}
