package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.ArrayList;
import java.util.List;

/** Clientbound scoreboard team packet (mirrors BungeeCord's {@code Team}, < 1.21.5). */
@Data
@NoArgsConstructor
public class Team extends DefinedPacket {

    private String name;
    private byte mode;
    private String displayName;
    private String prefix;
    private String suffix;
    private byte friendlyFire;
    private String nameTagVisibility;
    private String collisionRule;
    private int color;
    private List<String> players = new ArrayList<>();

    /** Remove-team packet (BungeeCord's {@code new Team(name)}). */
    public Team(String name) {
        this.name = name;
        this.mode = 1;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.name = readString(buf);
        this.mode = buf.readByte();
        if (mode == 0 || mode == 2) {
            this.displayName = readString(buf);
            if (protocolVersion < 393) {                               // pre-1.13
                this.prefix = readString(buf);
                this.suffix = readString(buf);
            }
            this.friendlyFire = buf.readByte();
            this.nameTagVisibility = readString(buf);
            if (protocolVersion >= 107) {                              // 1.9+
                this.collisionRule = readString(buf);
            }
            if (protocolVersion >= 393) {                              // 1.13+
                this.color = readVarInt(buf);
                this.prefix = readString(buf);
                this.suffix = readString(buf);
            } else {
                this.color = buf.readByte();
            }
        }
        if (mode == 0 || mode == 3 || mode == 4) {
            int count = readVarInt(buf);
            this.players = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                players.add(readString(buf));
            }
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeString(name, buf);
        buf.writeByte(mode);
        if (mode == 0 || mode == 2) {
            writeString(displayName, buf);
            if (protocolVersion < 393) {                               // pre-1.13
                writeString(prefix, buf);
                writeString(suffix, buf);
            }
            buf.writeByte(friendlyFire);
            writeString(nameTagVisibility, buf);
            if (protocolVersion >= 107) {                              // 1.9+
                writeString(collisionRule, buf);
            }
            if (protocolVersion >= 393) {                              // 1.13+
                writeVarInt(color, buf);
                writeString(prefix, buf);
                writeString(suffix, buf);
            } else {
                buf.writeByte(color);
            }
        }
        if (mode == 0 || mode == 3 || mode == 4) {
            writeVarInt(players.size(), buf);
            for (String player : players) {
                writeString(player, buf);
            }
        }
    }
}
