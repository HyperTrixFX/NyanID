package moe.koseirin.nyanruaineo.Minecraft.protocol.packet;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

/**
 * Clientbound的命令补全建议响应（1.13+ 的 {@code ClientboundCommandSuggestionsPacket}）。
 * 代理端为自己的命令构造此响应（每个建议写入时不附带工具提示）；
 * 后端响应则原样转发，且不在此处解码。
 */
public class TabCompleteResponse extends DefinedPacket {

    @Getter
    @Setter
    private int transactionId;
    @Getter
    @Setter
    private int start;
    @Getter
    @Setter
    private int length;
    @Getter
    @Setter
    private List<String> suggestions = List.of();

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.transactionId = readVarInt(buf);
        this.start = readVarInt(buf);
        this.length = readVarInt(buf);
        int count = readVarInt(buf);
        List<String> matches = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            matches.add(readString(buf));
            boolean hasTooltip = buf.readBoolean();
            if (hasTooltip) {
                // Skip the anonymous NBT tooltip component verbatim.
                skipNbt(buf);
            }
        }
        this.suggestions = matches;
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(this.transactionId, buf);
        writeVarInt(this.start, buf);
        writeVarInt(this.length, buf);
        writeVarInt(this.suggestions.size(), buf);
        for (String suggestion : this.suggestions) {
            writeString(suggestion, buf);
            buf.writeBoolean(false); // no tooltip
        }
    }

    /** Skips one anonymous NBT tag (used only when decoding a backend-supplied tooltip). */
    private void skipNbt(ByteBuf buf) {
        int type = buf.readByte() & 0xFF;
        if (type == 0) {
            return;
        }
        skipNbtPayload(buf, type);
    }

    private void skipNbtPayload(ByteBuf buf, int type) {
        switch (type) {
            case 1 -> buf.skipBytes(1);
            case 2 -> buf.skipBytes(2);
            case 3, 5 -> buf.skipBytes(4);
            case 4, 6 -> buf.skipBytes(8);
            case 7, 11, 12 -> {
                int n = readVarInt(buf);
                int size = switch (type) {
                    case 7 -> 1;
                    case 11 -> 4;
                    default -> 8;
                };
                buf.skipBytes(n * size);
            }
            case 8 -> readString(buf);
            case 9 -> {
                int elemType = buf.readByte() & 0xFF;
                int n = readVarInt(buf);
                for (int i = 0; i < n; i++) {
                    if (elemType == 0) {
                        continue;
                    }
                    if (elemType == 10) {
                        skipNbt(buf);
                    } else {
                        skipNbtPayload(buf, elemType);
                    }
                }
            }
            case 10 -> {
                while (true) {
                    int t = buf.getByte(buf.readerIndex()) & 0xFF;
                    if (t == 0) {
                        buf.skipBytes(1);
                        break;
                    }
                    readString(buf);
                    skipNbt(buf);
                }
            }
            default -> throw new IllegalStateException("Unknown NBT tag " + type);
        }
    }
}
