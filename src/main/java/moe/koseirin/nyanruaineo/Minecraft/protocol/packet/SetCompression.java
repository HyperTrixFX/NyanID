package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 这个包是登录阶段服务端发给客户端的 Set Compression（0x03），用来开启 zlib 压缩。
 * 服务端会告诉客户端一个阈值，从下一个包开始，任何大小大于等于这个阈值的包都会先压缩再发送。
 */
@Setter
@Getter
public class SetCompression extends DefinedPacket {

    private int threshold;

    public SetCompression() {
    }

    public SetCompression(int threshold) {
        this.threshold = threshold;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.threshold = readVarInt(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(this.threshold, buf);
    }

}
