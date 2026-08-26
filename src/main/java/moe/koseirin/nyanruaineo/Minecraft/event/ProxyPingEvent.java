package moe.koseirin.nyanruaineo.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 当客户端执行服务器列表 Ping（状态请求）时，在代理端触发此事件。
 * 该事件通过 EventBus 发布。
 */
public record ProxyPingEvent(int protocolVersion, String ip) {
}
