package moe.koseirin.nyanruaineo.Minecraft.connection;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.channel.Channel;
import lombok.Getter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 代理端网络连接的基础。
 */
@Getter
public abstract class Connection {

    private final Channel channel;

    protected Connection(Channel channel) {
        this.channel = channel;
    }

    public boolean isClosed() {
        return !channel.isActive();
    }

    /**
     * 通过此连接的管道写入数据包。通道的出站编码器根据数据包类解析数据包ID。
     */
    public void sendPacket(DefinedPacket packet) {
        channel.writeAndFlush(packet);
    }

    public void close() {
        if (channel.isActive()) {
            channel.close();
        }
    }
}
