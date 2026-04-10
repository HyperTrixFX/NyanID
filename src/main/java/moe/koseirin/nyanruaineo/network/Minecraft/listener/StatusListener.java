package moe.koseirin.nyanruaineo.network.Minecraft.listener;

import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PacketReceivedEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ProtocolVersion;
import moe.koseirin.nyanruaineo.network.Minecraft.network.handler.MinecraftProtocolHandler;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.PingRequestPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.PingResponsePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.StatusRequestPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.status.StatusResponsePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PingResponseProvider;
import moe.koseirin.nyanruaineo.network.Minecraft.util.PacketSender;
import org.springframework.stereotype.Component;

/*
 * @author KoseiRin_
 * awa
 */


@Component
@RequiredArgsConstructor
public class StatusListener {
    private final PingResponseProvider pingResponseProvider;
    private final PacketSender packetSender;

    @EventHeader
    public void onStatusRequest(PacketReceivedEvent event) {
        if (event.getPacket() instanceof StatusRequestPacket) {
            ChannelHandlerContext ctx = event.getCtx();
            ProtocolVersion version = ctx.channel().attr(MinecraftProtocolHandler.PROTOCOL_KEY).get();
            String json = pingResponseProvider.getPingJson(version);
            StatusResponsePacket response = new StatusResponsePacket(json);
            packetSender.send(ctx, response);
        }
    }

    @EventHeader
    public void onPingRequest(PacketReceivedEvent event) {
        if (event.getPacket() instanceof PingRequestPacket req) {
            PingResponsePacket resp = new PingResponsePacket(req.getPayload());
            packetSender.send(event.getCtx(), resp);
        }
    }
}
