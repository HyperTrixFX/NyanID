package moe.koseirin.nyanruaineo.network.Minecraft.network.packet.forge;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.network.codec.VarIntCodec;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.Packet;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ForgeModListPacket implements Packet {
    private boolean isServer;
    private List<Mod> mods = new ArrayList<>();

    public ForgeModListPacket(boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    public int packetId() {
        return 0xFC; // Forge Mod 列表包 ID
    }

    @Override
    public void encode(ByteBuf buf) {
    }

    @Override
    public void decode(ByteBuf buf) {
        if (isServer) {
            return;
        }
        // 客户端 -> 服务端：读取 Mod 列表
        // 1.7.10 - 1.12.2 格式：首先是 Mod 数量 (VarInt)，然后每个 Mod 有名称和版本（两个字符串）
        int count = VarIntCodec.readVarInt(buf);
        for (int i = 0; i < count; i++) {
            String modId = VarIntCodec.readString(buf);
            String version = VarIntCodec.readString(buf);
            mods.add(new Mod(modId, version));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mod {
        private String modId;
        private String version;
    }
}
