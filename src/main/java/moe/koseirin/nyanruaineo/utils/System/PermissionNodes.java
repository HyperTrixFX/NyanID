package moe.koseirin.nyanruaineo.utils.System;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

/**
 * <p>
 * 每个节点都是一段以 {@code .} 分隔的字符串。授权系统支持三种匹配规则：
 * <ol>
 *     <li>精确匹配：用户被授予 {@code minecraftproxy.command.server} 时，只有该节点返回 true。</li>
 *     <li>通配匹配：用户被授予 {@code minecraftproxy.command.*} 时，其下所有节点都返回 true。</li>
 *     <li>根节点 {@link #ROOT}（{@code "*"}）：拥有所有权限，等价于超级管理员。</li>
 * </ol>
 */
public final class PermissionNodes {

    private PermissionNodes() {
    }

    /** 根权限节点：授予后拥有所有权限。 */
    public static final String ROOT = "*";

    /** 代理管理权限：可管理后端服务器、授予/回收权限（HTTP 面板与所有代理指令）。 */
    public static final String PROXY_ADMIN = "minecraftproxy.admin";

    /** 代理命令前缀（用于授予“全部代理命令”）。 */
    public static final String COMMAND = "minecraftproxy.command";

    public static final String COMMAND_SERVER = "minecraftproxy.command.server";
    public static final String COMMAND_IP = "minecraftproxy.command.ip";
    public static final String COMMAND_LIST = "minecraftproxy.command.list";
    public static final String COMMAND_STATUS = "minecraftproxy.command.status";
    public static final String COMMAND_BROADCAST = "minecraftproxy.command.broadcast";
    public static final String COMMAND_KICK = "minecraftproxy.command.kick";
    public static final String COMMAND_BAN = "minecraftproxy.command.ban";

    public static final String NYANID_ADMIN = "nyanid.admin.cctop";
}
