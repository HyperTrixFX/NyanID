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
 * 向被代理的玩家发送聊天消息，并将消息广播给所有已连接的玩家。
 * 每位玩家会以其自身协议版本所对应的聊天数据包格式接收消息：1.19 版本以下使用 Legacy Chat，
 * 1.19 及以上版本使用 SystemChat（JSON 格式，1.20.3 以上则为 NBT 格式）。
 */
@Slf4j
@Component
public class PlayerMessageService {

    /**
     * 向单个玩家发送与其版本适配的聊天格式的消息。旧式的 {@code §} 颜色代码会被转换为真正的
     * JSON 组件（1.16 及以上版本的客户端不再在 JSON 文本中解析它们）。
     */
    public void sendMessage(UserConnection user, String text) {
        sendComponent(user, ChatComponentUtils.component(text));
    }

    /**
     * 向单个玩家发送原始 JSON 聊天组件，使用与其版本适配的聊天格式
     * 该组件会原样写入
     * 不进行旧式颜色代码转换。
     */
    public void sendRaw(UserConnection user, JSONObject component) {
        sendComponent(user, component);
    }

    /**
     * 向给定集合中的每位玩家（例如代理端的所有在线玩家）发送消息，
     * 并返回成功接收的玩家数量。发送失败的情况会被记录日志并跳过。
     */
    public int broadcast(Collection<UserConnection> users, String text) {
        int sent = broadcastComponents(users, ChatComponentUtils.component(text));
        log.info("Broadcast to {} players: {}", sent, text);
        return sent;
    }

    /**
     * 向给定集合中的每位玩家发送原始 JSON 聊天组件，
     * 并返回成功接收的玩家数量。
     */
    public int broadcastRaw(Collection<UserConnection> users, JSONObject component) {
        return broadcastComponents(users, component);
    }

    /** 共享广播核心：向集合中的每个（活跃）玩家发送一个组件。 */
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
     * 以玩家协议版本对应的聊天数据包格式写入一个 JSON 聊天组件：
     * 1.19 以下使用旧版聊天包（Legacy Chat），1.19 及以上使用 SystemChat
     * （JSON 格式，1.20.3 以上则为 NBT 格式）。
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

    /** 1.19 之前各协议版本的客户端bound聊天数据包ID */
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
     * 按协议版本区分的客户端bound SystemChat数据包ID；
     * 未明确列出条目的版本会继承相邻的较低版本）。
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
