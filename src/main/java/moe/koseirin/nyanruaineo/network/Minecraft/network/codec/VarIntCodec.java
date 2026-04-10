package moe.koseirin.nyanruaineo.network.Minecraft.network.codec;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class VarIntCodec {
    public static void writeVarInt(ByteBuf buf, int value) {
        do {
            byte temp = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) temp |= (byte) 0x80;
            buf.writeByte(temp);
        } while (value != 0);
    }

    public static int readVarInt(ByteBuf buf) {
        int result = 0;
        int shift = 0;
        byte b;
        do {
            b = buf.readByte();
            result |= (b & 0x7F) << shift;
            shift += 7;
            if (shift > 35) throw new RuntimeException("VarInt too big");
        } while ((b & 0x80) != 0);
        return result;
    }

    public static void writeString(ByteBuf buf, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(buf, bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString(ByteBuf buf) {
        int length = readVarInt(buf);
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
