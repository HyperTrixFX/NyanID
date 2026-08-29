package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

import java.util.ArrayDeque;

/**
 * 存储 Forge 服务器的数据并处理握手，与 BungeeCord 的 {@code ForgeServerHandler} 相对应。
 * 该处理器附加在正在连接的后端连接上；仅当后端被证实为 Forge 服务器后，才会暴露给客户端处理器。
 */
public class ForgeServerHandler {

    private final UserConnection con;
    private final ServerConnection ch;

    private ForgeServerHandshakeState state = ForgeServerHandshakeState.START;

    @Getter
    private boolean serverForge = false;

    private final ArrayDeque<PluginMessage> packetQueue = new ArrayDeque<>();

    public ForgeServerHandler(UserConnection con, ServerConnection ch) {
        this.con = con;
        this.ch = ch;
    }

    /**
     * 负责处理所有带有 FML 握手或 Forge 注册信息的插件消息。
     *
     * @param message 待处理的消息
     * @throws IllegalArgumentException 如果传入了非预期的数据包
     */
    public void handle(PluginMessage message) throws IllegalArgumentException {
        if (!message.getTag().equalsIgnoreCase(ForgeConstants.FML_HANDSHAKE_TAG)
                && !message.getTag().equalsIgnoreCase(ForgeConstants.FORGE_REGISTER)) {
            throw new IllegalArgumentException("Expecting a Forge REGISTER or FML Handshake packet.");
        }

        message.setAllowExtendedPacket(true); // FML allows extended packets so this must be enabled
        ForgeServerHandshakeState prevState = state;
        packetQueue.add(message);
        state = state.send(message, con);
        if (state != prevState) {                                     // send packets
            synchronized (packetQueue) {
                while (!packetQueue.isEmpty()) {
                    ForgeLogger.logServer(ForgeLogger.LogDirection.SENDING, prevState.name(), packetQueue.getFirst());
                    con.getForgeClientHandler().receive(packetQueue.removeFirst());
                }
            }
        }
    }

    /**
     * 这个方法负责从 ForgeClientHandler 那里拿到一个插件消息，然后转发给后端服务器。
     *
     * @param message 待处理的消息
     * @throws IllegalArgumentException 如果收到的包不合法
     */
    public void receive(PluginMessage message) throws IllegalArgumentException {
        state = state.handle(message, ch);
    }

    /**
     * 将该服务器标记为 Forge 服务器。此操作不可逆，不能将服务器恢复为原版。
     */
    public void setServerAsForgeServer() {
        serverForge = true;
    }

}
