package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

/**
 * 负责处理客户端握手数据包的处理器。
 */
interface IForgeClientPacketHandler<T extends Enum<T>> {

    /**
     * 这个方法用来处理从服务器那端发过来的数据包。
     *
     * @param message 收到的包
     * @param con     客户端的连接对象
     * @return 下一个握手状态
     * @throws IllegalArgumentException 如果收到的包不合法
     */
    T handle(PluginMessage message, UserConnection con) throws IllegalArgumentException;

    /**
     * 这个方法负责处理即将发往服务器那端的包。
     *
     * @param message 要发的包
     * @param con     客户端的连接
     * @return 下一个握手状态
     * @throws IllegalArgumentException 如果收到的包不合法
     */
    T send(PluginMessage message, UserConnection con) throws IllegalArgumentException;
}
