package moe.koseirin.nyanruaineo.network.Minecraft.util;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;

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
}
