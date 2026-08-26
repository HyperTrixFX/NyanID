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

/**
 * Serverbound客户端信息数据包（1.20.2+配置阶段，ID 0x00）。
 * 与BungeeCord的{@code ClientSettings}类似，但本实现不解析每个字段，而是捕获原始负载，以便在服务器切换时可以逐字节完美地重放到新的后端。
 * 代理端需要这样做，因为该数据包携带{@code skinParts}——如果不重放它，切换后的后端永远不知道客户端的皮肤层，并广播为全部关闭，这就是为什么披风和第二皮肤层在转移后停止渲染的原因。
 */
@Getter
@Setter
@NoArgsConstructor
public class ClientSettings extends DefinedPacket {

    private byte[] raw;

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.raw = new byte[buf.readableBytes()];
        buf.readBytes(raw);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeBytes(raw);
    }
}
