package moe.koseirin.nyanruaineo.network.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

@Getter
public class PacketReceivedEvent {
    private final ChannelHandlerContext ctx;
    private final Packet packet;

    public PacketReceivedEvent(ChannelHandlerContext ctx, Packet packet) {
        this.ctx = ctx;
        this.packet = packet;
    }

}