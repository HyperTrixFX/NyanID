package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

/**
 * 负责管理 Forge 客户端的握手流程和数据交互，和 BungeeCord 的 ForgeClientHandler 同理。
 * 它会记录客户端上报的模组列表，维护客户端握手的状态机，每当状态发生变化时，
 * 就把积压的握手包转交给服务端的处理器，好让这些包最终能发到后端服务器。
 */
public class ForgeClientHandler {

    private final UserConnection con;

    /**
     * 用户的模组列表，仅从第一次握手时获取。
     */
    @Setter
    @Getter
    private java.util.Map<String, String> clientModList = null;

    private final java.util.ArrayDeque<PluginMessage> packetQueue = new java.util.ArrayDeque<>();

    private ForgeClientHandshakeState state = ForgeClientHandshakeState.HELLO;

    private PluginMessage serverModList = null;
    private PluginMessage serverIdList = null;

    /**
     * 用于指示在客户端握手数据中是否检测到了 {@code \0FML\0} 标记（该标记用于识别 Forge 客户端）。
     */
    @Getter
    @Setter
    private boolean fmlTokenInHandshake = false;

    public ForgeClientHandler(UserConnection con) {
        this.con = con;
    }

    /**
     * 处理来自客户端的 Forge 数据包。
     *
     * @throws IllegalArgumentException 如果接收到无效数据包
     */
    public void handle(PluginMessage message) throws IllegalArgumentException {
        if (!message.getTag().equalsIgnoreCase(ForgeConstants.FML_HANDSHAKE_TAG)) {
            throw new IllegalArgumentException("Expecting a Forge Handshake packet.");
        }

        message.setAllowExtendedPacket(true); // FML allows extended packets so this must be enabled
        ForgeClientHandshakeState prevState = state;
        if (packetQueue.size() >= 128) {
            throw new IllegalStateException("Forge packet queue too big!");
        }
        packetQueue.add(message);
        state = state.send(message, con);
        if (state != prevState) {                                     // state finished, send packets
            synchronized (packetQueue) {
                while (!packetQueue.isEmpty()) {
                    ForgeLogger.logClient(ForgeLogger.LogDirection.SENDING, prevState.name(), packetQueue.getFirst());
                    // 通过服务器处理器将排队的握手数据包转发给后端。
                    // 与 BungeeCord 类似，该处理器在后台被确认为 Forge 服务器之前为 null，
                    // 而这一确认总是发生在客户端开始其握手之前。
                    if (con.getForgeServerHandler() != null) {
                        con.getForgeServerHandler().receive(packetQueue.removeFirst());
                    } else {
                        packetQueue.clear();
                        break;
                    }
                }
            }
        }
    }

    /**
     * 从 Forge 服务端的处理器那里拿到一个插件消息，然后转发给客户端。
     *
     * @throws IllegalArgumentException 如果收到的包不合法
     */
    public void receive(PluginMessage message) throws IllegalArgumentException {
        state = state.handle(message, con);
    }

    /**
     * 将客户端握手状态重置为 HELLO，并向客户端发送重置包。
     */
    public void resetHandshake() {
        state = ForgeClientHandshakeState.HELLO;
        con.sendPacket(ForgeConstants.FML_RESET_HANDSHAKE);
    }

    /**
     * 将服务器模组列表发送给客户端，或将其存储起来以供稍后发送。
     *
     * @throws IllegalArgumentException 如果数据包与预期不符
     */
    public void setServerModList(PluginMessage modList) throws IllegalArgumentException {
        if (!modList.getTag().equalsIgnoreCase(ForgeConstants.FML_HANDSHAKE_TAG) || modList.getData()[0] != 2) {
            throw new IllegalArgumentException("modList");
        }
        this.serverModList = modList;
    }

    /**
     * 将服务器 ID 列表发送给客户端，或将其存储起来以供稍后发送。
     *
     * @throws IllegalArgumentException 如果数据包与预期不符
     */
    public void setServerIdList(PluginMessage idList) throws IllegalArgumentException {
        if (!idList.getTag().equalsIgnoreCase(ForgeConstants.FML_HANDSHAKE_TAG) || idList.getData()[0] != 3) {
            throw new IllegalArgumentException("idList");
        }
        this.serverIdList = idList;
    }

    /**
     * 检查握手是否已经完成。
     */
    public boolean isHandshakeComplete() {
        return this.state == ForgeClientHandshakeState.DONE;
    }

    public void setHandshakeComplete() {
        this.state = ForgeClientHandshakeState.DONE;
    }

    /**
     * 返回我们是否知道该用户是 Forge 用户。在 FML 1.8 中，初始握手包含一个 "FML" 令牌，我们可以用它来判断用户是否为 Forge 1.8 用户。
     */
    public boolean isForgeUser() {
        return fmlTokenInHandshake || clientModList != null;
    }

    /** 包私有：握手状态机自行驱动状态转换（与 BungeeCord 行为一致）。 */
    void setState() {
        this.state = ForgeClientHandshakeState.HELLO;
    }
}
