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
 * 1.8之前的 Forge（FML 1.7）握手数据包（0x250），在 vanilla 登录开始之前的握手/登录阶段发送。
 * 携带一个频道（"FML|HS"）以及 FML 握手负载。代理端自身会终止此握手，并返回一个空模组列表。
 */
@Setter
@Getter
public class ForgeHandshake extends DefinedPacket {

    public static final String FML_HANDSHAKE_CHANNEL = "FML|HS";

    private String channel;
    private byte[] data;

    public ForgeHandshake() {
    }

    public ForgeHandshake(String channel, byte[] data) {
        this.channel = channel;
        this.data = data;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.channel = readString(buf);
        this.data = new byte[buf.readableBytes()];
        buf.readBytes(this.data);
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(channel, buf);
        buf.writeBytes(data);
    }

}
