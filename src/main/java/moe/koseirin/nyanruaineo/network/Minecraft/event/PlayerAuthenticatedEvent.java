package moe.koseirin.nyanruaineo.network.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PlayerAuthenticatedEvent {
    private final Channel channel;
    private final String username;
    private final java.util.UUID uuid;
    private final boolean onlineMode;

}