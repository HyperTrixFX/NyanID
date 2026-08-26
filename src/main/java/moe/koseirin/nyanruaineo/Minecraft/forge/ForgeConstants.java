package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

import java.util.regex.Pattern;

/**
 * 存放 FML 握手所用的频道名称和握手数据，参考 BungeeCord 的 ForgeConstants。
 */
public final class ForgeConstants {

    private ForgeConstants() {
    }

    /** Forge 用这个频道来向代理端声明它支持哪些频道。 */
    public static final String FORGE_REGISTER = "FORGE";

    /** 即 FML（Forge Mod Loader）所使用的频道名称。 */
    public static final String FML_TAG = "FML";
    public static final String FML_HANDSHAKE_TAG = "FML|HS";
    public static final String FML_REGISTER = "REGISTER";

    /** 追加到握手主机名后面的 FML 1.8 握手令牌。
     * 在 Forge 1.8+ 中，客户端发起握手时，会在握手数据包中的 hostname 字段（即服务器地址）后面追加一个 \0FML\0 令牌（部分版本还包含 mod 列表的 hash 值）。
     * 代理端可以通过检测这个令牌来识别是否为 Forge 客户端，并将其剥离后还原出真实的主机名用于路由。
     *
     */
    public static final String FML_HANDSHAKE_TOKEN = "\0FML\0";

    public static final PluginMessage FML_RESET_HANDSHAKE = new PluginMessage(FML_HANDSHAKE_TAG, new byte[]
            {-2, 0});
    public static final PluginMessage FML_ACK = new PluginMessage(FML_HANDSHAKE_TAG, new byte[]
            {-1, 0});
    public static final PluginMessage FML_START_CLIENT_HANDSHAKE = new PluginMessage(FML_HANDSHAKE_TAG, new byte[]
            {0, 1});
    public static final PluginMessage FML_START_SERVER_HANDSHAKE = new PluginMessage(FML_HANDSHAKE_TAG, new byte[]
            {1, 1});
    public static final PluginMessage FML_EMPTY_MOD_LIST = new PluginMessage(FML_HANDSHAKE_TAG, new byte[]
            {2, 0});

    /**
     * 用于从 FML 握手中提取版本信息的正则表达式（构建号为第 4 个捕获组）。
     */
    public static final Pattern FML_HANDSHAKE_VERSION_REGEX = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");
}
