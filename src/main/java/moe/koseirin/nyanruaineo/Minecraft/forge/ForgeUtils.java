package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * 提供一些处理 FML 握手数据（Payload）的辅助方法。
 */
public final class ForgeUtils {

    private ForgeUtils() {
    }

    /**
     * 从 {@code REGISTER} 负载中获取已注册的 FML 频道。
     */
    public static Set<String> readRegisteredChannels(PluginMessage pluginMessage) {
        String channels = new String(pluginMessage.getData(), StandardCharsets.UTF_8);
        return Set.of(channels.split("\0"));
    }

    /**
     * 解析 FML 握手阶段的 ModList 数据包（数据包类型标识符为 2）。该数据包格式为：一个整数计数，
     * 后面跟着若干个（modId, version）配对。
     */
    public static Map<String, String> readModList(PluginMessage pluginMessage) {
        Map<String, String> modTags = new HashMap<>();
        ByteBuf payload = Unpooled.wrappedBuffer(pluginMessage.getData());
        byte discriminator = payload.readByte();
        if (discriminator == 2) {                                    // ModList
            ByteBuf buffer = payload.slice();
            int modCount = DefinedPacket.readVarInt(buffer, 2);
            for (int i = 0; i < modCount; i++) {
                modTags.put(DefinedPacket.readString(buffer), DefinedPacket.readString(buffer));
            }
        }
        return modTags;
    }

    /**
     * 从模组列表中获取 FML 的构建号（build number），若无法确定则返回 0。
     * 对于 1.7.10 版本中 1405 之后的构建，FML 将其版本硬编码为 7.10.99.99，
     * 此时将改用 Forge 自身的构建号。
     */
    public static int getFmlBuildNumber(Map<String, String> modList) {
        if (modList.containsKey("FML")) {
            String fmlVersion = modList.get("FML");

            if (fmlVersion.equals("7.10.99.99")) {
                Matcher matcher = ForgeConstants.FML_HANDSHAKE_VERSION_REGEX.matcher(modList.get("Forge"));
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(4));
                }
            } else {
                Matcher matcher = ForgeConstants.FML_HANDSHAKE_VERSION_REGEX.matcher(fmlVersion);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(4));
                }
            }
        }
        return 0;
    }
}
