package moe.koseirin.nyanruaineo.Minecraft.protocol;

/*
 * @author KoseiRin_
 * awa
 */

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

    /**
     * Reads a VarInt consuming at most {@code maxBytes} bytes, mirroring BungeeCord's bounded
     * variant (used by the FML mod list, whose count field must stay tiny).
     */
    public static int readVarInt(ByteBuf input, int maxBytes) {
        int out = 0;
        int bytes = 0;
        byte in;
        while (true) {
            in = input.readByte();
            out |= (in & 0x7F) << (bytes++ * 7);
            if (bytes > maxBytes) {
                throw new RuntimeException("VarInt too big");
            }
            if ((in & 0x80) != 0x80) {
                break;
            }
        }
        return out;
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

    /**
     * Reads a string whose UTF-8 byte length and decoded character count are both bounded,
     * mirroring BungeeCord's {@code readString(ByteBuf, int)} (used for legacy plugin channel
     * names).
     */
    public static String readString(ByteBuf input, int maxLen) {
        int length = readVarInt(input);
        if (length > maxLen * 4) {
            throw new IllegalArgumentException("Cannot receive string longer than " + maxLen * 4 + " bytes");
        }
        byte[] bytes = new byte[length];
        input.readBytes(bytes);
        String value = new String(bytes, StandardCharsets.UTF_8);
        if (value.length() > maxLen) {
            throw new IllegalArgumentException("Cannot receive string longer than " + maxLen);
        }
        return value;
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

    /** Copies every remaining byte of the buffer into a fresh array, mirroring BungeeCord. */
    public static byte[] toArray(ByteBuf buf) {
        byte[] ret = new byte[buf.readableBytes()];
        buf.readBytes(ret);
        return ret;
    }

    /**
     * Decode this packet from the buffer, with the direction the packet travelled. The default
     * implementation ignores the direction; packets whose wire format depends on it (e.g. the
     * {@code PluginMessage} payload cap) override this.
     */
    public void read(ByteBuf buf, Direction direction, int protocolVersion) {
        read(buf, protocolVersion);
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

    /**
     * The protocol phase this packet moves the connection to once it is decoded/encoded, or
     * {@code null} to leave the current phase unchanged. Mirrors BungeeCord's
     * {@code nextProtocol()} (LoginAcknowledged/StartConfiguration → CONFIGURATION,
     * FinishConfiguration → GAME). The decoder/encoder apply this automatically.
     */
    public Protocol nextProtocol() {
        return null;
    }
}
