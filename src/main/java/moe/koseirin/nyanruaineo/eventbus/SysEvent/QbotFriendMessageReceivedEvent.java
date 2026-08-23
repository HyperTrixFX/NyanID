package moe.koseirin.nyanruaineo.eventbus.SysEvent;

/*
 * @author KoseiRin_
 * awa
 */

import io.github.kloping.qqbot.api.v2.FriendMessageEvent;

public record QbotFriendMessageReceivedEvent(FriendMessageEvent event) {
}
