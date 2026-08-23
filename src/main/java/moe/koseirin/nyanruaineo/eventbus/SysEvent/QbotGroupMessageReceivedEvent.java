package moe.koseirin.nyanruaineo.eventbus.SysEvent;

/*
 * @author KoseiRin_
 * awa
 */

import io.github.kloping.qqbot.api.v2.GroupMessageEvent;

public record QbotGroupMessageReceivedEvent(GroupMessageEvent event) {
}
