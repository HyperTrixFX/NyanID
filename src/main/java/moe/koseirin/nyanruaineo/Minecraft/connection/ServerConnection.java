package moe.koseirin.nyanruaineo.Minecraft.connection;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

/**
 * 代理连接的服务器端部分。
 */
@Getter
public class ServerConnection extends Connection {

    private final String host;
    private final int port;
    @Setter
    private UserConnection user;
    /**
     * 后端是否被证实为 Forge 服务器（即它注册了 `FML|HS` 频道）。
     * 与 BungeeCord 中那个名义上存在但始终为 false 的字段不同，该字段是实际跟踪的，
     * 以便 UpstreamBridge 能仅对原版后端应用 Forge 的雷霆而大之数据包防护。
     */
    private boolean forgeServer;

    public ServerConnection(Channel channel, String host, int port) {
        super(channel);
        this.host = host;
        this.port = port;
    }

    /**
     * 将该后端标记为 Forge 服务器；此操作不可撤销（与 BungeeCord 保持一致）。
     */
    public void setServerAsForgeServer() {
        this.forgeServer = true;
    }

    /**
     * 向此后端发送插件消息，与 BungeeCord 的
     * {@code Server.sendData(String, byte[])} 方法行为一致(滑的喵~)。
     */
    public void sendData(String channel, byte[] data) {
        sendPacket(new PluginMessage(channel, data));
    }

}
