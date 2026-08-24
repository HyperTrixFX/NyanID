package moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.DefinedPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Clientbound player info update packet (1.19.3-1.20.2: protocols 761-764, where the display
 * name component is still a JSON string), mirroring BungeeCord's {@code PlayerListItemUpdate}.
 * One actions bitmask (a single byte) applies to every entry in the packet. Registered so the
 * proxy can decorate TabList display names on 1.19.3+ clients; 1.20.3+ (NBT display names) stays
 * relayed raw.
 */
@Data
@NoArgsConstructor
public class PlayerInfoUpdate extends DefinedPacket {

    public static final int ACTION_ADD_PLAYER = 0;
    public static final int ACTION_INITIALIZE_CHAT = 1;
    public static final int ACTION_UPDATE_GAMEMODE = 2;
    public static final int ACTION_UPDATE_LISTED = 3;
    public static final int ACTION_UPDATE_LATENCY = 4;
    public static final int ACTION_UPDATE_DISPLAY_NAME = 5;

    /** Action bitset (one byte on the wire). */
    private int actions;
    private List<Item> items = new ArrayList<>();

    public PlayerInfoUpdate(int actions, List<Item> items) {
        this.actions = actions;
        this.items = items;
    }

    public boolean hasAction(int bit) {
        return (actions & (1 << bit)) != 0;
    }

    @Override
    public void read(ByteBuf buf, int protocolVersion) {
        this.actions = buf.readUnsignedByte();
        int count = readVarInt(buf);
        this.items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Item item = new Item();
            item.setUuid(readUUID(buf));

            if (hasAction(ACTION_ADD_PLAYER)) {
                item.setUsername(readString(buf));
                int propertyCount = readVarInt(buf);
                List<PlayerListItem.Property> properties = new ArrayList<>(propertyCount);
                for (int p = 0; p < propertyCount; p++) {
                    String name = readString(buf);
                    String value = readString(buf);
                    String signature = null;
                    if (buf.readBoolean()) {
                        signature = readString(buf);
                    }
                    properties.add(new PlayerListItem.Property(name, value, signature));
                }
                item.setProperties(properties);
            }
            if (hasAction(ACTION_INITIALIZE_CHAT)) {
                if (buf.readBoolean()) {
                    item.setChatSessionId(readUUID(buf));
                    item.setPublicKeyExpiry(buf.readLong());
                    item.setPublicKey(readArray(buf));
                    item.setPublicKeySignature(readArray(buf));
                }
            }
            if (hasAction(ACTION_UPDATE_GAMEMODE)) {
                item.setGamemode(readVarInt(buf));
            }
            if (hasAction(ACTION_UPDATE_LISTED)) {
                item.setListed(buf.readBoolean());
            }
            if (hasAction(ACTION_UPDATE_LATENCY)) {
                item.setPing(readVarInt(buf));
            }
            if (hasAction(ACTION_UPDATE_DISPLAY_NAME)) {
                if (buf.readBoolean()) {
                    item.setDisplayName(readString(buf));
                }
            }
            items.add(item);
        }
    }

    @Override
    public void write(ByteBuf buf, int protocolVersion) {
        buf.writeByte(actions);
        writeVarInt(items.size(), buf);
        for (Item item : items) {
            writeUUID(item.uuid, buf);

            if (hasAction(ACTION_ADD_PLAYER)) {
                writeString(item.username, buf);
                List<PlayerListItem.Property> properties = item.getProperties();
                writeVarInt(properties == null ? 0 : properties.size(), buf);
                if (properties != null) {
                    for (PlayerListItem.Property property : properties) {
                        writeString(property.getName(), buf);
                        writeString(property.getValue(), buf);
                        buf.writeBoolean(property.getSignature() != null);
                        if (property.getSignature() != null) {
                            writeString(property.getSignature(), buf);
                        }
                    }
                }
            }
            if (hasAction(ACTION_INITIALIZE_CHAT)) {
                buf.writeBoolean(item.chatSessionId != null);
                if (item.chatSessionId != null) {
                    writeUUID(item.chatSessionId, buf);
                    buf.writeLong(item.publicKeyExpiry);
                    writeArray(item.publicKey == null ? new byte[0] : item.publicKey, buf);
                    writeArray(item.publicKeySignature == null ? new byte[0] : item.publicKeySignature, buf);
                }
            }
            if (hasAction(ACTION_UPDATE_GAMEMODE)) {
                writeVarInt(item.gamemode, buf);
            }
            if (hasAction(ACTION_UPDATE_LISTED)) {
                buf.writeBoolean(item.listed);
            }
            if (hasAction(ACTION_UPDATE_LATENCY)) {
                writeVarInt(item.ping, buf);
            }
            if (hasAction(ACTION_UPDATE_DISPLAY_NAME)) {
                buf.writeBoolean(item.displayName != null);
                if (item.displayName != null) {
                    writeString(item.displayName, buf);
                }
            }
        }
    }

    /** One player entry of the update packet. */
    @Data
    @NoArgsConstructor
    public static class Item {

        private UUID uuid;
        private String username;
        private List<PlayerListItem.Property> properties = new ArrayList<>();
        private UUID chatSessionId;
        private long publicKeyExpiry;
        private byte[] publicKey;
        private byte[] publicKeySignature;
        private int gamemode;
        private boolean listed;
        private int ping;
        /** JSON chat component (null = none). */
        private String displayName;
    }
}
