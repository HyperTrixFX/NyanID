package moe.koseirin.nyanruaineo.Minecraft.command;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 一个可以执行代理命令的实体，类似于 BungeeCord 的 {@code CommandSender}。
 * {@link PlayerCommandSender} 支持一个在线的代理玩家；控制台发送者可以稍后添加。
 */
public interface CommandSender {

    /** 顾名思义。*/
    String getName();

    /** 向该发送者发送一条普通聊天消息。 */
    void sendMessage(String message);

    /** 这个发送者是否拥有给定的权限节点 */
    boolean hasPermission(String permission);
}
