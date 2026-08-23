package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clientbound player list item packet in the legacy format shared by 1.8-1.18.2 (one action per
 * packet plus per-entry fields), mirroring BungeeCord's {@code PlayerListItem}. Registered so the
 * proxy can intercept TabList entries (e.g. apply a configured prefix/suffix to display names);
 * the 1.19+ player info packets are relayed raw.
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
