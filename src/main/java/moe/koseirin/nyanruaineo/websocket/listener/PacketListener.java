package moe.koseirin.nyanruaineo.websocket.listener;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.Packet.Client.BindAccountPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.CheckBindPacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.HeartbeatResponsePacket;
import moe.koseirin.nyanruaineo.network.Packet.Client.UpdateOnlinePacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.CheckBindResponsePacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.HeartbeatPacket;
import moe.koseirin.nyanruaineo.network.Packet.Server.UpdateOnlineResponsePacket;
import moe.koseirin.nyanruaineo.network.utils.KeyManager;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;
import moe.koseirin.nyanruaineo.network.utils.SM4Util;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.websocket.Handler.BungeeWebSocketHandler;
import moe.koseirin.nyanruaineo.websocket.event.PacketReceivedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/*
 * @author KoseiRin_
 * awa
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PacketListener {

    private final RedisService redisService;
    private final AccountsRepository accountsRepository;
    private final KeyManager KeyManager;


    @EventHeader
    public void handlePacket(PacketReceivedEvent event) throws IOException {
        WebSocketSession session = event.session();
        switch (event.packet()) {
            case HeartbeatPacket _ignored ->
                    sendPacket(session, new HeartbeatResponsePacket());

            case UpdateOnlinePacket p -> {
                try {
                    redisService.setValueWithExpiration("Online-" + p.getServername(), p.getOnline(), 30, TimeUnit.SECONDS);
                    sendPacket(session, new UpdateOnlineResponsePacket(true));
                } catch (Exception e) {
                    log.error("更新在线人数失败喵", e);
                    sendPacket(session, new UpdateOnlineResponsePacket(false));
                }
            }

            case BindAccountPacket p -> {
                try {
                    redisService.setValueWithExpiration(p.getCode(), p.getUuid(), 180, TimeUnit.SECONDS);
                    sendPacket(session, new UpdateOnlineResponsePacket(true));
                } catch (Exception e) {
                    log.error("绑定账号失败喵", e);
                    sendPacket(session, new UpdateOnlineResponsePacket(false));
                }
            }

            case CheckBindPacket p -> {
                try {
                    Accounts accounts = accountsRepository.GetUser(p.getUuid());
                    if (accounts == null) {
                        sendPacket(session, new CheckBindResponsePacket(false, null, null, null));
                    } else {
                        sendPacket(session, new CheckBindResponsePacket(true, accounts.getBind(), accounts.getUid(), accounts.getUsername()));
                    }
                } catch (Exception e) {
                    log.error("检查绑定失败喵", e);
                    session.close(CloseStatus.PROTOCOL_ERROR);
                }
            }

            default -> {
                log.error("未知的包类型喵: {}", event.packet().getClass().getSimpleName());
                session.close(new CloseStatus(4500, "10001"));
            }
        }
    }




    private void sendPacket(WebSocketSession session, Packet packet) throws IOException {
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
}
