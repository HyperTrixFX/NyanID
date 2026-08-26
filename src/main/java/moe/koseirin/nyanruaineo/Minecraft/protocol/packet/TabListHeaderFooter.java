package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.protocol.ProtocolConstants;
import moe.koseirin.nyanruaineo.Minecraft.util.ChatComponentUtils;

/**
 * 这是服务端发给客户端的 TabList 页眉和页脚数据包，从 1.8 版本就有了。
 * 在 1.20.3 以前，页眉和页脚是 JSON 字符串格式；
 * 到了 1.20.3 及之后，改成了匿名 NBT 组件格式。
 * 代理端会拦截这个包，这样就能在所有版本里自由替换页眉/页脚的内容。
 */
@Data
@NoArgsConstructor
public class TabListHeaderFooter extends DefinedPacket {

    private String header;
    private String footer;
    /** Raw NBT component (1.20.3+). */
    private byte[] headerNbt;
    private byte[] footerNbt;

    public TabListHeaderFooter(String header, String footer) {
        this.header = header;
        this.footer = footer;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            this.headerNbt = ChatComponentUtils.readNbtComponentBytes(buf);
            this.footerNbt = ChatComponentUtils.readNbtComponentBytes(buf);
        } else {
            this.header = readString(buf);
            this.footer = readString(buf);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_20_3) {
            buf.writeBytes(headerNbt == null ? new byte[] { 0 } : headerNbt);
            buf.writeBytes(footerNbt == null ? new byte[] { 0 } : footerNbt);
        } else {
            writeString(header == null ? "" : header, buf);
            writeString(footer == null ? "" : footer, buf);
        }
    }
}
