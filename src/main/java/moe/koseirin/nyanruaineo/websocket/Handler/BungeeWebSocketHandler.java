package moe.koseirin.nyanruaineo.websocket.Handler;

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.EventBus;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.Packet.Client.HeartbeatResponsePacket;
import moe.koseirin.nyanruaineo.network.utils.KeyManager;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;
import moe.koseirin.nyanruaineo.network.utils.PacketRegistry;
import moe.koseirin.nyanruaineo.network.utils.SM4Util;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.ServerListRepository;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.websocket.event.PacketReceivedEvent;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class BungeeWebSocketHandler extends BinaryWebSocketHandler {

    private final ServerListRepository serverListRepository;
    private final KeyManager KeyManager;
    private final PacketRegistry packetRegistry;
    private final EventBus eventBus;
    private final Map<String, WebSocketSession> authenticatedSessions = new ConcurrentHashMap<>();

    public BungeeWebSocketHandler(ServerListRepository serverListRepository, RedisService redisService, AccountsRepository accountsRepository, KeyManager KeyManager, PacketRegistry packetRegistry, EventBus eventBus) {
        this.serverListRepository = serverListRepository;
        this.KeyManager = KeyManager;
        this.packetRegistry = packetRegistry;
        this.eventBus = eventBus;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        String sid = (String) attributes.get("sid");
        String key = (String) attributes.get("key");

        if (sid == null || key == null) {
            log.error("握手阶段未提供认证信息喵（X-SID / X-KEY），关闭连接喵，SessionId: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        log.info("检测到BungeeCord连接请求喵，Sid: [{}], SessionId: {}", sid, session.getId());

        String token = serverListRepository.findTokenByServerUid(sid);
        if (token != null && Objects.equals(key, token)) {
            authenticatedSessions.put(session.getId(), session);
            log.info("BungeeCord连接认证成功喵，SessionId: {}", session.getId());
        } else {
            log.error("认证失败，无效的 sid 或 key，SessionId喵: {}", session.getId());
            session.close(new CloseStatus(4500, "10001"));
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, @NonNull BinaryMessage message) throws Exception {
        if (!authenticatedSessions.containsKey(session.getId())) {
            log.warn("未认证的会话尝试发送消息，关闭连接喵，SessionId: {}", session.getId());
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        ByteBuffer payload = message.getPayload();
        byte[] encryptedData = new byte[payload.remaining()];
        payload.get(encryptedData);

        // check empty data
        if (encryptedData.length == 0) {
            log.error("收到空消息，关闭连接喵，SessionId: {}", session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        byte[] key = KeyManager.getKey(session.getId());
        byte[] plainData;
        try {
            plainData = SM4Util.decrypt(encryptedData, key);
        } catch (IllegalArgumentException e) {
            log.error("解密参数错误喵: {}, SessionId: {}", e.getMessage(), session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        } catch (javax.crypto.IllegalBlockSizeException e) {
            log.error("解密失败，密文长度不正确（可能不是16的倍数）喵: {}, SessionId: {}", e.getMessage(), session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        } catch (javax.crypto.BadPaddingException e) {
            log.error("解密失败，填充错误（可能密钥不正确）喵: {}, SessionId: {}", e.getMessage(), session.getId());
            session.close(CloseStatus.BAD_DATA);
            return;
        } catch (Exception e) {
            log.error("解密消息失败，关闭连接喵，SessionId: {}", session.getId(), e);
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        ByteBuf buf = Unpooled.wrappedBuffer(plainData);
        try {
            int packetId = PacketCodecUtil.readVarInt(buf);
            Packet packet = packetRegistry.createPacket(packetId);
            packet.decode(buf);
            eventBus.postAsync(new PacketReceivedEvent(session,packet));
        } catch (Exception e) {
            log.error("处理数据包异常喵，SessionId: {}", session.getId(), e);
            session.close(CloseStatus.PROTOCOL_ERROR);
        } finally {
            buf.release();
        }
    }

    public void sendPacket(WebSocketSession session, Packet packet) throws IOException {
        ByteBuf buf = Unpooled.buffer();
        try {
            PacketCodecUtil.writeVarInt(buf, packet.packetId());
            packet.encode(buf);

            byte[] plainData = new byte[buf.readableBytes()];
            buf.readBytes(plainData);

            byte[] key = KeyManager.getKey(session.getId());
            byte[] encryptedData = SM4Util.encrypt(plainData, key);

            session.sendMessage(new BinaryMessage(ByteBuffer.wrap(encryptedData)));
        } catch (Exception e) {
            log.error("发送数据包失败喵", e);
            throw new IOException("发送失败喵", e);
        } finally {
            buf.release();
        }
    }

    public void broadcastPacket(Packet packet) {
        for (WebSocketSession session : authenticatedSessions.values()) {
            try {
                if (session.isOpen()) {
                    sendPacket(session, packet);
                }
            } catch (IOException e) {
                log.error("广播包失败喵，SessionId: {}", session.getId(), e);
            }
        }
    }

    public void broadcastHeartbeat() {
        HeartbeatResponsePacket packet = new HeartbeatResponsePacket();
        for (WebSocketSession session : authenticatedSessions.values()) {
            try {
                if (session.isOpen()) {
                    sendPacket(session, packet);
                }
            } catch (IOException e) {
                log.error("广播心跳失败喵，SessionId: {}", session.getId(), e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NonNull CloseStatus status) {
        authenticatedSessions.remove(session.getId());
        log.warn("Bungee连接断开喵，SessionId: {}, 状态: {}", session.getId(), status);
    }


}
