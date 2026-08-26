package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.protocol.DefinedPacket;
import moe.koseirin.nyanruaineo.Minecraft.util.ChatComponentUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Sends chat messages to proxied players and broadcasts them across every connected player
 * (BungeeCord {@code broadcast} style). Each player receives the message in the chat packet format
 * of their own protocol version: legacy Chat below 1.19, SystemChat (JSON or 1.20.3+ NBT) above.
 */
@Slf4j
@Component
public class PlayerMessageService {

    /**
     * Sends a chat message to one player in their version-appropriate chat format. Legacy
     * {@code §} colour codes are converted into real JSON components (1.16+ clients no longer
     * interpret them inside JSON text).
     */
    public void sendMessage(UserConnection user, String text) {
        sendComponent(user, ChatComponentUtils.component(text));
    }

    /**
     * Sends a raw JSON chat component to one player in their version-appropriate chat format
     * (BungeeCord {@code MessageRaw} style): the component is written verbatim, with no legacy
     * colour-code conversion.
     */
    public void sendRaw(UserConnection user, JSONObject component) {
        sendComponent(user, component);
    }

    /**
     * Sends the message to every player in the given set (e.g. the proxy's online players) and
     * returns how many received it. Failed sends are logged and skipped.
     */
    public int broadcast(Collection<UserConnection> users, String text) {
        int sent = broadcastComponents(users, ChatComponentUtils.component(text));
        log.info("Broadcast to {} players: {}", sent, text);
        return sent;
    }

    /**
     * Sends a raw JSON chat component to every player in the given set (BungeeCord
     * {@code MessageRaw} style) and returns how many received it.
     */
    public int broadcastRaw(Collection<UserConnection> users, JSONObject component) {
        return broadcastComponents(users, component);
    }

    /** Shared broadcast core: sends one component to every (active) player of the set. */
    private int broadcastComponents(Collection<UserConnection> users, JSONObject component) {
        int sent = 0;
        for (UserConnection user : users) {
            if (user.getChannel() == null || !user.getChannel().isActive()) {
                continue;
            }
            try {
                sendComponent(user, component);
                sent++;
            } catch (Exception e) {
                log.warn("Failed to broadcast message to {}", user.getUsername(), e);
            }
        }
        return sent;
    }

    /**
     * Writes one JSON chat component in the chat packet format of the player's protocol version:
     * legacy Chat below 1.19, SystemChat (JSON or 1.20.3+ NBT) above.
     */
    private void sendComponent(UserConnection user, JSONObject component) {
        if (user.getChannel() == null || !user.getChannel().isActive()) {
            return;
        }
        int protocolVersion = user.getProtocolVersion();

        if (protocolVersion >= 759) {                                  // 1.19+ SystemChat
            ByteBuf buf = Unpooled.buffer();
            DefinedPacket.writeVarInt(systemChatId(protocolVersion), buf);
            if (protocolVersion >= 765) {                              // 1.20.3+ NBT component
                ChatComponentUtils.writeNbtComponent(buf, component);
            } else {
                DefinedPacket.writeString(component.toJSONString(), buf);
            }
            if (protocolVersion >= 760) {                              // 1.19.1+ boolean position
                buf.writeBoolean(false);                               // chat box, not action bar
            } else {
                DefinedPacket.writeVarInt(1, buf);                     // 1.19 position: SYSTEM
            }
            user.getChannel().writeAndFlush(buf);
            return;
        }

        int packetId = chatPacketId(protocolVersion);

        ByteBuf buf = Unpooled.buffer();
        DefinedPacket.writeVarInt(packetId, buf);
        DefinedPacket.writeString(component.toJSONString(), buf);
        buf.writeByte(0);                                              // position: chat box
        if (protocolVersion >= 735) {                                  // 1.16+ sender UUID
            buf.writeLong(0);
            buf.writeLong(0);
        }
        user.getChannel().writeAndFlush(buf);
    }

    /** Pre-1.19 clientbound Chat packet ids per protocol version (BungeeCord mappings). */
    private static int chatPacketId(int protocolVersion) {
        if (protocolVersion >= 755) {
            return 0x0F;                                               // 1.17-1.18.2
        }
        if (protocolVersion >= 735) {
            return 0x0E;                                               // 1.16-1.16.5
        }
        if (protocolVersion >= 573) {
            return 0x0F;                                               // 1.15-1.15.2
        }
        if (protocolVersion >= 477) {
            return 0x0E;                                               // 1.13-1.14.4
        }
        if (protocolVersion >= 107) {
            return 0x0F;                                               // 1.9-1.12.2
        }
        return 0x02;                                                   // 1.7.10-1.8.9
    }

    /**
     * Clientbound SystemChat ids per protocol version (BungeeCord {@code TO_CLIENT SystemChat}
     * mappings; versions without an entry inherit the nearest lower one).
     */
    private static int systemChatId(int protocolVersion) {
        if (protocolVersion >= 775) {
            return 0x79;                                               // 26.1+
        }
        if (protocolVersion >= 773) {
            return 0x77;                                               // 1.21.9+
        }
        if (protocolVersion >= 770) {
            return 0x72;                                               // 1.21.5+
        }
        if (protocolVersion >= 768) {
            return 0x73;                                               // 1.21.2+
        }
        if (protocolVersion >= 766) {
            return 0x6C;                                               // 1.20.5+
        }
        if (protocolVersion >= 765) {
            return 0x69;                                               // 1.20.3+
        }
        if (protocolVersion >= 764) {
            return 0x67;                                               // 1.20.2
        }
        if (protocolVersion >= 762) {
            return 0x64;                                               // 1.19.4-1.20.1
        }
        if (protocolVersion >= 761) {
            return 0x60;                                               // 1.19.3
        }
        if (protocolVersion >= 760) {
            return 0x62;                                               // 1.19.1-1.19.2
        }
        return 0x5F;                                                   // 1.19
    }
}
