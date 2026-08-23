package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

/**
 * Clientbound TabList header/footer packet (1.8-1.20.2, where the two components are JSON
 * strings). Mirrors BungeeCord's {@code PlayerListHeaderFooter}. Registered so the proxy can
 * intercept and replace the header/footer; 1.20.3+ (NBT components) is relayed raw.
 */
@Data
@NoArgsConstructor
public class TabListHeaderFooter extends DefinedPacket {

    private String header;
    private String footer;

    public TabListHeaderFooter(String header, String footer) {
        this.header = header;
        this.footer = footer;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.header = readString(buf);
        this.footer = readString(buf);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(header == null ? "" : header, buf);
        writeString(footer == null ? "" : footer, buf);
    }
}
