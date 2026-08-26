package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 这是 1.19.3+ 版本中，服务器发往客户端的玩家信息移除数据包.
 * 代理端注册处理这个包，是为了在它修改 TabList 显示名称时，
 * 能及时把被移除玩家的名字从缓存里清理掉，避免残留。
 */
@Data
@NoArgsConstructor
public class PlayerInfoRemove extends DefinedPacket {

    private List<UUID> uuids = new ArrayList<>();

    public PlayerInfoRemove(List<UUID> uuids) {
        this.uuids = uuids;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        int count = readVarInt(buf);
        this.uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(readUUID(buf));
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(uuids.size(), buf);
        for (UUID uuid : uuids) {
            writeUUID(uuid, buf);
        }
    }
}
