package moe.koseirin.nyanruaineo.network.Minecraft.network.handler;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.EventBus;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PacketReceivedEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ConnectionState;
import moe.koseirin.nyanruaineo.network.Minecraft.network.ProtocolVersion;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.handshake.HandshakePacket;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;

@Slf4j
@Component
@ChannelHandler.Sharable
public class MinecraftProtocolHandler extends SimpleChannelInboundHandler<Packet> {

    public static final AttributeKey<ConnectionState> STATE_KEY = AttributeKey.valueOf("state");
    public static final AttributeKey<ProtocolVersion> PROTOCOL_KEY = AttributeKey.valueOf("protocol");
    public static final AttributeKey<Cipher> ENCRYPTION_CIPHER = AttributeKey.valueOf("encryption_cipher");
    public static final AttributeKey<Cipher> DECRYPTION_CIPHER = AttributeKey.valueOf("decryption_cipher");
    private final EventBus eventBus;

    public MinecraftProtocolHandler(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.channel().attr(STATE_KEY).set(ConnectionState.HANDSHAKE);
        ctx.channel().attr(PROTOCOL_KEY).set(ProtocolVersion.UNKNOWN);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        eventBus.post(new PacketReceivedEvent(ctx, packet));

        if (packet instanceof HandshakePacket handshake) {
            ProtocolVersion version = ProtocolVersion.fromProtocol(handshake.getProtocolVersion());
            ctx.channel().attr(PROTOCOL_KEY).set(version);
            ConnectionState next = handshake.getNextState() == 1 ? ConnectionState.STATUS : ConnectionState.LOGIN;
            ctx.channel().attr(STATE_KEY).set(next);
        }
        ctx.fireChannelRead(packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Protocol error", cause);
        ctx.close();
    }
}
