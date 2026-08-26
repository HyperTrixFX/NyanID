package moe.koseirin.nyanruaineo.Minecraft.util;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Minimal NBT support for the proxy: reads a whole NBT tag from the wire and returns its raw
 * bytes so tags like the JoinGame dimension registry round-trip byte-for-byte. The root may be
 * named (type byte + name + payload, the pre-1.19.4 form) or unnamed (type byte + payload, the
 * 1.19.4+ form) — the reader tries the named form first and falls back to the unnamed form,
 * validating the top-level structure (allocation-free) so the correct form is detected without
 * building an object tree.
 */
public final class NbtUtils {

    private static final Set<String> REGISTRY_KEYS = Set.of("minecraft:dimension_type", "minecraft:worldgen/biome");

    private NbtUtils() {
    }

    /** Reads one NBT tag (named or unnamed) as raw bytes, expecting a registry codec compound. */
    public static byte[] readTagRaw(ByteBuf buf) {
        return readTagRaw(buf, true);
    }

    /**
     * Reads one NBT tag (named or unnamed) as raw bytes. With {@code registry} the root compound
     * must contain registry keys; otherwise any non-empty compound is accepted.
     */
    public static byte[] readTagRaw(ByteBuf buf, boolean registry) {
        int start = buf.readerIndex();
        buf.markReaderIndex();
        if (trySkip(buf, true, registry)) {
            return slice(buf, start);
        }
        buf.resetReaderIndex();
        if (trySkip(buf, false, registry)) {
            return slice(buf, start);
        }
        throw new IllegalArgumentException("Could not parse the NBT tag (neither the named nor the unnamed form matched)");
    }

    private static byte[] slice(ByteBuf buf, int start) {
        int length = buf.readerIndex() - start;
        byte[] raw = new byte[length];
        buf.getBytes(start, raw);
        return raw;
    }

    /**
     * Skips one tag while validating the top level: the root must be a non-empty compound and,
     * for {@code registry}, contain at least one registry key among its direct children. Nested
     * payloads are skipped without any allocation.
     */
    private static boolean trySkip(ByteBuf buf, boolean named, boolean registry) {
        try {
            if (buf.readableBytes() < 3) {
                return false;
            }
            int rootType = buf.readUnsignedByte();
            if (rootType != 10) {                                      // root must be a compound
                return false;
            }
            if (named) {
                int rootNameLength = buf.readUnsignedShort();
                if (rootNameLength > buf.readableBytes()) {
                    return false;
                }
                buf.skipBytes(rootNameLength);                         // root name
            }

            boolean sawChild = false;
            boolean sawRegistryKey = !registry;
            int childType;
            while (buf.readableBytes() >= 1 && (childType = buf.readUnsignedByte()) != 0) {
                sawChild = true;
                if (buf.readableBytes() < 2) {
                    return false;
                }
                int nameLength = buf.readUnsignedShort();
                if (nameLength > buf.readableBytes()) {
                    return false;
                }
                if (registry && !sawRegistryKey) {
                    byte[] name = new byte[nameLength];
                    buf.readBytes(name);
                    sawRegistryKey = REGISTRY_KEYS.contains(new String(name, StandardCharsets.UTF_8));
                } else {
                    buf.skipBytes(nameLength);
                }
                skipPayload(buf, childType);
            }
            return sawChild && sawRegistryKey;
        } catch (RuntimeException e) {
            // Misparsed form (e.g. named parse of an unnamed tag): fall back to the other form.
            return false;
        }
    }

    private static void skipPayload(ByteBuf buf, int type) {
        switch (type) {
            case 1: // byte
                buf.skipBytes(1);
                break;
            case 2: // short
                buf.skipBytes(2);
                break;
            case 3: // int
                buf.skipBytes(4);
                break;
            case 4: // long
            case 6: // double
                buf.skipBytes(8);
                break;
            case 5: // float
                buf.skipBytes(4);
                break;
            case 7: { // byte array
                int length = buf.readInt();
                buf.skipBytes(length);
                break;
            }
            case 8: // string
                buf.skipBytes(buf.readUnsignedShort());
                break;
            case 9: { // list
                int elementType = buf.readUnsignedByte();
                int count = buf.readInt();
                if (elementType == 0) {
                    return;
                }
                for (int i = 0; i < count; i++) {
                    skipPayload(buf, elementType);
                }
                break;
            }
            case 10: // compound
                int childType;
                while ((childType = buf.readUnsignedByte()) != 0) {
                    int nameLength = buf.readUnsignedShort();
                    buf.skipBytes(nameLength);                         // child name bytes
                    skipPayload(buf, childType);
                }
                break;
            case 11: { // int array
                int length = buf.readInt();
                buf.skipBytes((int) (length * 4L));
                break;
            }
            case 12: { // long array
                int length = buf.readInt();
                buf.skipBytes((int) (length * 8L));
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown NBT tag type: " + type);
        }
    }
}
