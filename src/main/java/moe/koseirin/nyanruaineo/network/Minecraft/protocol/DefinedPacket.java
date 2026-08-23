package moe.koseirin.nyanruaineo.network.Minecraft.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Base class for every Minecraft protocol packet, mirroring BungeeCord's {@code DefinedPacket}.
 * <p>
 * Subclasses only need to implement the {@link #read(ByteBuf, int)} / {@link #write(ByteBuf, int)}
 * variants; the version-less variants delegate to them so callers may use either.
 */
public abstract class DefinedPacket {

    public static void writeVarInt(int value, ByteBuf output) {
        do {
            byte temp = (byte) (value & 0x7F);
            value >>>= 7;
            if (value != 0) {
                temp |= (byte) 0x80;
            }
            output.writeByte(temp);
        } while (value != 0);
    }

    public static int readVarInt(ByteBuf input) {
        int result = 0;
        int shift = 0;
        byte b;
        do {
            if (shift > 35) {
                throw new IllegalArgumentException("VarInt too big");
            }
            b = input.readByte();
            result |= (b & 0x7F) << shift;
            shift += 7;
        } while ((b & 0x80) != 0);
        return result;
    }

    public static void writeString(String value, ByteBuf output) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length, output);
        output.writeBytes(bytes);
    }

    public static String readString(ByteBuf input) {
        int length = readVarInt(input);
        byte[] bytes = new byte[length];
        input.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeArray(byte[] value, ByteBuf output) {
        writeVarInt(value.length, output);
        output.writeBytes(value);
    }

    public static byte[] readArray(ByteBuf input) {
        int length = readVarInt(input);
        byte[] bytes = new byte[length];
        input.readBytes(bytes);
        return bytes;
    }

    public static void writeUUID(UUID value, ByteBuf output) {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    public static UUID readUUID(ByteBuf input) {
        return new UUID(input.readLong(), input.readLong());
    }

    /**
     * Decode this packet from the buffer. The protocol version is provided so version-dependent
     * packets can switch their wire format.
     */
    public void read(ByteBuf buf, int protocolVersion) {
        throw new UnsupportedOperationException("Packet must implement read");
    }

    public void read(ByteBuf buf) {
        read(buf, -1);
    }

    /**
     * Encode this packet to the buffer.
     */
    public void write(ByteBuf buf, int protocolVersion) {
        throw new UnsupportedOperationException("Packet must implement write");
    }

    public void write(ByteBuf buf) {
        write(buf, -1);
    }
}
