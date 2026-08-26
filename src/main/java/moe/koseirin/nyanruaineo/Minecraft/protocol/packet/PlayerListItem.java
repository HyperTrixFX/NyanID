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
 * 这是 1.8 到 1.18.2 版本使用的传统玩家列表条目数据包（每个包只包含一种动作，
 * 但条目字段是每个条目独立的）。
 * 代理端处理这个包是为了拦截 TabList 条目，比如给显示名称加上配置好的前缀或后缀；
 * 而 1.19 及以上版本改用新的玩家信息包，代理不处理，直接原样转发。
 */
@Data
@NoArgsConstructor
public class PlayerListItem extends DefinedPacket {

    public static final int ACTION_ADD_PLAYER = 0;
    public static final int ACTION_UPDATE_GAMEMODE = 1;
    public static final int ACTION_UPDATE_LATENCY = 2;
    public static final int ACTION_UPDATE_DISPLAY_NAME = 3;
    public static final int ACTION_REMOVE_PLAYER = 4;

    private int action;
    private List<Item> items = new ArrayList<>();

    public PlayerListItem(int action, List<Item> items) {
        this.action = action;
        this.items = items;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.action = readVarInt(buf);
        int count = readVarInt(buf);
        this.items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.setUuid(readUUID(buf));
            switch (action) {
                case ACTION_ADD_PLAYER:
                    item.setUsername(readString(buf));
                    int propertyCount = readVarInt(buf);
                    List<Property> properties = new ArrayList<>(propertyCount);
                    for (int p = 0; p < propertyCount; p++) {
                        String name = readString(buf);
                        String value = readString(buf);
                        String signature = null;
                        if (buf.readBoolean()) {
                            signature = readString(buf);
                        }
                        properties.add(new Property(name, value, signature));
                    }
                    item.setProperties(properties);
                    item.setGamemode(readVarInt(buf));
                    item.setPing(readVarInt(buf));
                    if (buf.readBoolean()) {
                        item.setDisplayName(readString(buf));
                    }
                    break;
                case ACTION_UPDATE_GAMEMODE:
                    item.setGamemode(readVarInt(buf));
                    break;
                case ACTION_UPDATE_LATENCY:
                    item.setPing(readVarInt(buf));
                    break;
                case ACTION_UPDATE_DISPLAY_NAME:
                    if (buf.readBoolean()) {
                        item.setDisplayName(readString(buf));
                    }
                    break;
                default:
                    break;
            }
            items.add(item);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        writeVarInt(action, buf);
        writeVarInt(items.size(), buf);
        for (Item item : items) {
            writeUUID(item.uuid, buf);
            switch (action) {
                case ACTION_ADD_PLAYER:
                    writeString(item.username, buf);
                    List<Property> properties = item.getProperties();
                    writeVarInt(properties == null ? 0 : properties.size(), buf);
                    if (properties != null) {
                        for (Property property : properties) {
                            writeString(property.name, buf);
                            writeString(property.value, buf);
                            buf.writeBoolean(property.signature != null);
                            if (property.signature != null) {
                                writeString(property.signature, buf);
                            }
                        }
                    }
                    writeVarInt(item.gamemode, buf);
                    writeVarInt(item.ping, buf);
                    buf.writeBoolean(item.displayName != null);
                    if (item.displayName != null) {
                        writeString(item.displayName, buf);
                    }
                    break;
                case ACTION_UPDATE_GAMEMODE:
                    writeVarInt(item.gamemode, buf);
                    break;
                case ACTION_UPDATE_LATENCY:
                    writeVarInt(item.ping, buf);
                    break;
                case ACTION_UPDATE_DISPLAY_NAME:
                    buf.writeBoolean(item.displayName != null);
                    if (item.displayName != null) {
                        writeString(item.displayName, buf);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /** One player entry of the list packet. */
    @Data
    @NoArgsConstructor
    public static class Item {

        private UUID uuid;
        private String username;
        private List<Property> properties = new ArrayList<>();
        private int gamemode;
        private int ping;
        /** JSON chat component (null = none). */
        private String displayName;
    }

    /** A profile property (skin/cape), as stored on the session server. */
    @Data
    @NoArgsConstructor
    public static class Property {

        private String name;
        private String value;
        private String signature;

        public Property(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }
    }
}
