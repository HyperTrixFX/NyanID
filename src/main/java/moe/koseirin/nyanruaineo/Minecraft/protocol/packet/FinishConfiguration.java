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
 * 这是 FinishConfiguration 包（1.20.2 及以上，客户端和服务端都能发），用来结束配置阶段。
 * 包本身是空的，但编解码器看到它就会把连接状态切到 GAME。
 */
@Getter
@Setter
@NoArgsConstructor
public class FinishConfiguration extends DefinedPacket {

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
    }

    @Override
    public Protocol nextProtocol() {
        return Protocol.GAME;
    }
}
