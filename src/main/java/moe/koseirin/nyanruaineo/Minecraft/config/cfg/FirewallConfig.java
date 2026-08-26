package moe.koseirin.nyanruaineo.Minecraft.config.cfg;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Data;

/**
 * 连接防火墙配置，存储在 {@code proxy.firewall} 下。用于限制每个 IP 的连接速率、
 * 并发连接数和登录尝试次数，并在超限时临时封禁该 IP，防止机器人把代理（以及 Mojang
 * 会话服务器）冲爆。
 */
@Data
public class FirewallConfig {

    /** 是否启用防火墙。 */
    private boolean enabled;
    /** 每个 IP 每秒允许的新连接数（超过即封禁）。 */
    private int maxConnectionsPerSecond;
    /** 每个 IP 允许的并发连接数（超过即封禁）。 */
    private int maxConcurrentPerIp;
    /** 每个 IP 每分钟允许的登录尝试次数（超过即封禁）。 */
    private int maxLoginAttemptsPerMinute;
    /** 每个 IP 每分钟允许的登录失败次数（超过即封禁）。 */
    private int maxLoginFailuresPerMinute;
    /** 触发封禁后的封禁时长（秒）。 */
    private int banDurationSeconds;
}
