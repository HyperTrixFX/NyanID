package moe.koseirin.nyanruaineo.Minecraft.util;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared chat-component helpers: parses legacy {@code §} colour codes into a fastjson2 component
 * tree and serialises that tree either as a JSON string (all versions below 1.20.3) or as network
 * NBT (1.20.3+). Used by the proxy's chat replies and the TabList interception.
 */
public final class ChatComponentUtils {

    private ChatComponentUtils() {
    }

    /**
     * Parses a legacy {@code §}-coded string and returns the chat component tree. The root object
     * mirrors the first styled section and carries the remaining sections in its {@code extra}
     * array, so the tree serialises to both JSON and NBT forms.
     */
    public static JSONObject component(String legacyText) {
        List<Section> sections = parseLegacy(legacyText);
        JSONObject root = new JSONObject();
        fillComponent(root, sections.isEmpty() ? new Section() : sections.get(0));
        if (sections.size() > 1) {
            JSONArray extra = new JSONArray();
            for (int i = 1; i < sections.size(); i++) {
                JSONObject child = new JSONObject();
                fillComponent(child, sections.get(i));
                extra.add(child);
            }
            root.put("extra", extra);
        }
        return root;
    }

    /**
     * Writes a component tree as a network NBT chat component (root compound tag: type byte +
     * payload), the form used by 1.20.3+.
     */
    public static void writeNbtComponent(ByteBuf buf, JSONObject component) {
        buf.writeByte(10);                                             // TAG_Compound
        writeNbtCompoundBody(buf, component);
    }

    /**
     * Reads one anonymous NBT chat component (1.20.3+) back into a component tree. Returns an empty
     * component when the buffer carries an empty tag.
     */
    public static JSONObject readNbtComponent(ByteBuf buf) {
        int type = buf.readUnsignedByte();
        if (type == 0) {
            return new JSONObject();
        }
        if (type != 10) {
            throw new IllegalStateException("Expected TAG_Compound chat component, got " + type);
        }
        JSONObject component = new JSONObject();
        readNbtCompoundBody(buf, component);
        return component;
    }

    /** Reads one anonymous NBT chat component and returns its raw bytes (type byte + payload). */
    public static byte[] readNbtComponentBytes(ByteBuf buf) {
        int start = buf.readerIndex();
        int type = buf.readUnsignedByte();
        if (type == 0) {
            return new byte[] { 0 };
        }
        if (type != 10) {
            throw new IllegalStateException("Expected TAG_Compound chat component, got " + type);
        }
        skipNbtCompoundBody(buf);
        byte[] bytes = new byte[buf.readerIndex() - start];
        buf.getBytes(start, bytes);
        return bytes;
    }

    /** Encodes a component tree as the raw network NBT bytes used by 1.20.3+. */
    public static byte[] writeNbtComponentBytes(JSONObject component) {
        io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
        try {
            writeNbtComponent(buf, component);
            return DefinedPacket.toArray(buf);
        } finally {
            buf.release();
        }
    }

    // ---- legacy § colour-code parsing ------------------------------------------------

    /** One styled run of text inside a chat component. */
    private static final class Section {

        String text;
        String color;
        boolean bold, italic, underlined, strikethrough, obfuscated;

        Section copy() {
            Section s = new Section();
            s.color = color;
            s.bold = bold;
            s.italic = italic;
            s.underlined = underlined;
            s.strikethrough = strikethrough;
            s.obfuscated = obfuscated;
            return s;
        }
    }

    private static List<Section> parseLegacy(String text) {
        List<Section> sections = new ArrayList<>();
        Section current = new Section();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00A7' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(++i));
                if (code == 'r') {
                    addSection(sections, current, sb);
                    current = new Section();
                    continue;
                }
                String color = colorFor(code);
                if (color != null) {
                    addSection(sections, current, sb);
                    current = new Section();
                    current.color = color;
                    continue;
                }
                switch (code) {
                    case 'k':
                        current = split(sections, current, sb);
                        current.obfuscated = true;
                        continue;
                    case 'l':
                        current = split(sections, current, sb);
                        current.bold = true;
                        continue;
                    case 'm':
                        current = split(sections, current, sb);
                        current.strikethrough = true;
                        continue;
                    case 'n':
                        current = split(sections, current, sb);
                        current.underlined = true;
                        continue;
                    case 'o':
                        current = split(sections, current, sb);
                        current.italic = true;
                        continue;
                    default:
                        break;
                }
                // Unknown code: keep both characters literally.
                sb.append(c).append(code);
            } else {
                sb.append(c);
            }
        }
        addSection(sections, current, sb);
        return sections;
    }

    private static String colorFor(char code) {
        return switch (code) {
            case '0' -> "black";
            case '1' -> "dark_blue";
            case '2' -> "dark_green";
            case '3' -> "dark_aqua";
            case '4' -> "dark_red";
            case '5' -> "dark_purple";
            case '6' -> "gold";
            case '7' -> "gray";
            case '8' -> "dark_gray";
            case '9' -> "blue";
            case 'a' -> "green";
            case 'b' -> "aqua";
            case 'c' -> "red";
            case 'd' -> "light_purple";
            case 'e' -> "yellow";
            case 'f' -> "white";
            default -> null;
        };
    }

    private static void addSection(List<Section> sections, Section current, StringBuilder sb) {
        if (sb.length() > 0) {
            current.text = sb.toString();
            sections.add(current);
            sb.setLength(0);
        }
    }

    private static Section split(List<Section> sections, Section current, StringBuilder sb) {
        if (sb.length() > 0) {
            current.text = sb.toString();
            sections.add(current);
            sb.setLength(0);
            return current.copy();
        }
        return current;
    }

    private static void fillComponent(JSONObject obj, Section s) {
        obj.put("text", s.text == null ? "" : s.text);
        if (s.color != null) {
            obj.put("color", s.color);
        }
        if (s.bold) {
            obj.put("bold", true);
        }
        if (s.italic) {
            obj.put("italic", true);
        }
        if (s.underlined) {
            obj.put("underlined", true);
        }
        if (s.strikethrough) {
            obj.put("strikethrough", true);
        }
        if (s.obfuscated) {
            obj.put("obfuscated", true);
        }
    }

    // ---- 1.20.3+ NBT component encoding ------------------------------------------------

    /** Walks the fastjson2 component tree and emits NBT tags (vanilla network form). */
    private static void writeNbtCompoundBody(ByteBuf buf, JSONObject obj) {
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String text) {
                writeNbtStringEntry(buf, key, text);
            } else if (value instanceof Boolean flag) {
                writeNbtEntryHeader(buf, 1, key);                      // TAG_Byte
                buf.writeByte(flag ? 1 : 0);
            } else if (value instanceof JSONArray extra) {
                writeNbtEntryHeader(buf, 9, key);                      // TAG_List
                buf.writeByte(10);                                     // element type TAG_Compound
                buf.writeInt(extra.size());
                for (Object item : extra) {
                    if (item instanceof JSONObject child) {
                        writeNbtCompoundBody(buf, child);
                    }
                }
            } else if (value instanceof JSONObject child) {
                writeNbtEntryHeader(buf, 10, key);                     // TAG_Compound
                writeNbtCompoundBody(buf, child);
            }
        }
        buf.writeByte(0);                                              // TAG_End
    }

    private static void writeNbtStringEntry(ByteBuf buf, String name, String value) {
        writeNbtEntryHeader(buf, 8, name);
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    private static void writeNbtEntryHeader(ByteBuf buf, int type, String name) {
        buf.writeByte(type);
        byte[] bytes = name.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    // ---- 1.20.3+ NBT component decoding ------------------------------------------------

    /** Reads a named compound body (entries until TAG_End) into the given component object. */
    private static void readNbtCompoundBody(ByteBuf buf, JSONObject obj) {
        while (true) {
            int type = buf.readUnsignedByte();
            if (type == 0) {
                return; // TAG_End
            }
            String name = readNbtName(buf);
            switch (type) {
                case 1 -> obj.put(name, buf.readUnsignedByte() != 0);              // TAG_Byte -> boolean
                case 8 -> obj.put(name, readNbtString(buf));                       // TAG_String
                case 9 -> {                                                        // TAG_List
                    int elemType = buf.readUnsignedByte();
                    int size = buf.readInt();
                    JSONArray array = new JSONArray();
                    for (int i = 0; i < size; i++) {
                        if (elemType == 10) {
                            JSONObject child = new JSONObject();
                            readNbtCompoundBody(buf, child);
                            array.add(child);
                        } else if (elemType == 8) {
                            array.add(readNbtString(buf));
                        } else {
                            skipNbtPayload(buf, elemType);
                        }
                    }
                    obj.put(name, array);
                }
                case 10 -> {                                                     // TAG_Compound
                    JSONObject child = new JSONObject();
                    readNbtCompoundBody(buf, child);
                    obj.put(name, child);
                }
                default -> skipNbtPayload(buf, type);
            }
        }
    }

    private static String readNbtName(ByteBuf buf) {
        int length = buf.readUnsignedShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static String readNbtString(ByteBuf buf) {
        int length = buf.readUnsignedShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** Skips one compound body (entries until TAG_End). */
    private static void skipNbtCompoundBody(ByteBuf buf) {
        while (true) {
            int type = buf.readUnsignedByte();
            if (type == 0) {
                return;
            }
            readNbtName(buf);
            skipNbtPayload(buf, type);
        }
    }

    /** Skips one NBT payload (never expected inside a chat component, kept for robustness). */
    private static void skipNbtPayload(ByteBuf buf, int type) {
        switch (type) {
            case 1 -> buf.skipBytes(1);
            case 2 -> buf.skipBytes(2);
            case 3, 5 -> buf.skipBytes(4);
            case 4, 6 -> buf.skipBytes(8);
            case 7, 11, 12 -> buf.skipBytes(buf.readInt() * (type == 7 ? 1 : type == 11 ? 4 : 8));
            case 8 -> readNbtString(buf);
            case 9 -> {
                int elemType = buf.readUnsignedByte();
                int size = buf.readInt();
                for (int i = 0; i < size; i++) {
                    skipNbtPayload(buf, elemType);
                }
            }
            case 10 -> {
                while (true) {
                    int tag = buf.readUnsignedByte();
                    if (tag == 0) {
                        return;
                    }
                    readNbtName(buf);
                    skipNbtPayload(buf, tag);
                }
            }
            default -> throw new IllegalStateException("Unknown NBT tag " + type);
        }
    }
}
