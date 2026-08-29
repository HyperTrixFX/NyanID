package moe.koseirin.nyanruaineo.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.UUID;

/**
 * 玩家断开连接（无论是主动还是被动）时，代理端会触发该事件，原因不限。
 * 注意：若客户端还未完成登录就断开，用户名会是 {@code null}。
 * 该事件会通过 EventBus 广播。
 */
public record PlayerDisconnectEvent(String username, UUID uuid) {
}
