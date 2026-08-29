package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Direction;
import moe.koseirin.nyanruaineo.Minecraft.protocol.ProtocolConstants;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.util.Locale;

/**
 * 这个类对应 BungeeCord 里的 PluginMessage，用来处理插件消息（自定义数据包）。
 * 在 1.13 及以上版本，频道名需要带命名空间（比如 minecraft:brand）。代理端会自动把 Bukkit 插件里用的旧频道名
 * （比如 "BungeeCord"）转成线路上用的现代格式（"bungeecord:main"），也会给没有冒号的旧频道加上 "legacy:" 前缀。
 * 这套转换逻辑和 BungeeCord 的 MODERNISE 函数一模一样，所以插件开发者不需要关心底层协议版本，用同一个频道名就行了。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PluginMessage extends DefinedPacket {

    /** 代理端自身的插件频道，用于旧版客户端（及代理内部）。 */
    public static final String BUNGEE_CHANNEL_LEGACY = "BungeeCord";
    /** 代理端自身的插件频道，用于旧版客户端（及代理内部）。 同一频道在 1.13+ 版本中的命名空间格式名称。 */
    public static final String BUNGEE_CHANNEL_MODERN = "bungeecord:main";

    /**
     * Transforms a channel name between the legacy and modern (1.13+) notations, mirroring
     * BungeeCord's {@code PluginMessage.MODERNISE}.
     */
    public static String modernise(String tag) {
        if (tag.equals(BUNGEE_CHANNEL_LEGACY)) {
            return BUNGEE_CHANNEL_MODERN;
        }
        if (tag.equals(BUNGEE_CHANNEL_MODERN)) {
            return BUNGEE_CHANNEL_LEGACY;
        }
        // Already namespaced names are left untouched; anything else gets the legacy: prefix, so
        // Bukkit-style plugin channels keep working on 1.13+.
        if (tag.indexOf(':') != -1) {
            return tag;
        }
        return "legacy:" + tag.toLowerCase(Locale.ROOT);
    }

    private String tag;
    private byte[] data;

    /** Convenience constructor for ordinary (non-extended) plugin messages. */
    public PluginMessage(String tag, byte[] data) {
        this.tag = tag;
        this.data = data;
    }

    /**
     * 这个开关允许把当前数据包当作“扩展包”来发送。保留它是为了和 BungeeCord 的行为保持一致：
     * Forge 的握手处理器会在 FML 握手包上把它设为 true（这些包可能超过 32KB 的游戏阶段上限），
     * 但现在编码器其实已经不再读取这个字段了。
     */
    private boolean allowExtendedPacket = false;

    @Override
    public void read(ByteBuf buf, Direction direction, int protocolVersion) {
        tag = (protocolVersion >= ProtocolConstants.MINECRAFT_1_13)
                ? modernise(readString(buf))
                : readString(buf, 20);
        // 高版本的模组客户端（比如 NeoForge、Forge、Fabric）经常会发送雷霆大的自定义数据包，远超原版规定的 32KiB 上限（比如冻结的注册表快照、注册表映射、枪械数据包、配置文件等等）。
        // 代理端绝对不能丢弃这些包，所以收发两个方向都要放开大小限制，这样才能保证模组频道的流量原封不动地传给客户端或后端服务器。
        int maxSize = 0x200000; // 2 MiB, both directions
        if (buf.readableBytes() > maxSize) {
            throw new IllegalArgumentException("Payload too large");
        }
        data = new byte[buf.readableBytes()];
        buf.readBytes(data);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString((protocolVersion >= ProtocolConstants.MINECRAFT_1_13) ? modernise(tag) : tag, buf);
        buf.writeBytes(data);
    }

    /** Streams the payload, mirroring BungeeCord's {@code getStream()} (used by the Forge handlers). */
    public DataInput getStream() {
        return new DataInputStream(new ByteArrayInputStream(data));
    }
}
