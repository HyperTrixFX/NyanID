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
 * 这是客户端发给服务端的 LoginAcknowledged 包（1.20.2 以上才有），
 * 客户端收到 Login Success 之后立刻就会发这个，意思是“我准备好进配置阶段了”。
 * 这个包是空的，但解码的时候会把连接状态切到 CONFIGURATION。
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginAcknowledged extends DefinedPacket {

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
