package moe.koseirin.nyanruaineo.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.UUID;

/**
 * 在客户端通过身份验证（包括正版验证或离线 UUID 生成）之后，但还没连到后端服务器之前，代理端会触发这个事件。
 * 该事件通过 EventBus 广播。
 */
public record PlayerLoginEvent(String username, UUID uuid, int protocolVersion, String requestedServer, String ip) {
}
