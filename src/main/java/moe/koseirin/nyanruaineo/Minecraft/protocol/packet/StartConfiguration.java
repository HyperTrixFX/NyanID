package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.Protocol;

/**
 * 这个包是发给客户端的 StartConfiguration（1.20.2 以上才有），作用是把客户端重新拉回配置阶段（比如切换服务器的时候会用）。
 * 包本身是空的，但编码器看到它就会把连接状态切到 CONFIGURATION。
 */
@Getter
@Setter
@NoArgsConstructor
public class StartConfiguration extends DefinedPacket {

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
    }

    @Override
    public Protocol nextProtocol() {
        return Protocol.CONFIGURATION;
    }
}
