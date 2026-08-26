package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

/**
 * 服务端这边的握手包处理器。
 */
interface IForgeServerPacketHandler<T extends Enum<T>> {

    /**
     * 处理从客户端那边发过来的数据包。
     *
     * @param message 收到的包
     * @param ch      后端连接对象
     * @return 下一个握手状态
     * @throws IllegalArgumentException 如果收到的包不合法
     */
    T handle(PluginMessage message, ServerConnection ch) throws IllegalArgumentException;

    /**
     * 处理即将发给客户端那边的包。
     *
     * @param message 要发的包
     * @param con     客户端连接
     * @return 下一个握手状态
     * @throws IllegalArgumentException 如果包不合法
     */
    T send(PluginMessage message, UserConnection con) throws IllegalArgumentException;
}
