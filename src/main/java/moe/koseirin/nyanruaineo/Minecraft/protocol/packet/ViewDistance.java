package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * 这是服务端发给客户端的视野距离（View Distance）数据包。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewDistance extends DefinedPacket {

    private int distance;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.distance = readVarInt(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(distance, buf);
    }
}
