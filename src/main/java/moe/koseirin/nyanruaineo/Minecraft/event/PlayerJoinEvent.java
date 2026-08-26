package moe.koseirin.nyanruaineo.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.UUID;

/**
 * 当后端确认登录成功，开始进入游戏数据中转阶段时，代理端会触发该事件。
 * 事件通过 EventBus 广播。
 */
public record PlayerJoinEvent(String username, UUID uuid, int protocolVersion, String serverHost, int serverPort) {
}
