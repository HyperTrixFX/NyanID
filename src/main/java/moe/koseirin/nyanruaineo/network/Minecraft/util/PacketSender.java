package moe.koseirin.nyanruaineo.network.Minecraft.util;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;
import org.springframework.stereotype.Component;

/*
 * @author KoseiRin_
 * awa
 */
@Component
public class PacketSender {

    public void send(ChannelHandlerContext ctx, Packet packet) {
        ByteBuf buf = ctx.alloc().buffer();
        ByteBuf temp = ctx.alloc().buffer();
        try {
            VarIntCodec.writeVarInt(temp, packet.packetId());
            packet.encode(temp);
            VarIntCodec.writeVarInt(buf, temp.readableBytes());
            buf.writeBytes(temp);
            ctx.writeAndFlush(buf);
        } catch (Exception e) {
            buf.release();
            throw new RuntimeException("Failed to send packet", e);
        } finally {
            temp.release();
        }
    }

    public void send(Channel channel, Packet packet) {
        ByteBuf buf = channel.alloc().buffer();
        ByteBuf temp = channel.alloc().buffer();
        try {
            VarIntCodec.writeVarInt(temp, packet.packetId());
            packet.encode(temp);
            VarIntCodec.writeVarInt(buf, temp.readableBytes());
            buf.writeBytes(temp);
            channel.writeAndFlush(buf);
        } catch (Exception e) {
            buf.release();
            throw new RuntimeException("Failed to send packet", e);
        } finally {
            temp.release();
        }
    }
}

