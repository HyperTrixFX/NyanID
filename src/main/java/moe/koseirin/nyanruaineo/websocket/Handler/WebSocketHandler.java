package moe.koseirin.nyanruaineo.websocket.Handler;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import moe.koseirin.nyanruaineo.network.Interface.Packet;
import moe.koseirin.nyanruaineo.network.utils.KeyManager;
import moe.koseirin.nyanruaineo.network.utils.PacketCodecUtil;
import moe.koseirin.nyanruaineo.network.utils.PacketRegistry;
import moe.koseirin.nyanruaineo.network.utils.SM4Util;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

//e.g

//@Component
//public class WebSocketHandler extends BinaryWebSocketHandler {
//
//    private final KeyManager keyManager;
//    private final PacketRegistry packetRegistry;
//
//    public WebSocketHandler(KeyManager keyManager, PacketRegistry packetRegistry) {
//        this.keyManager = keyManager;
//        this.packetRegistry = packetRegistry;
//    }
//
//    @Override
//    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
//        byte[] encryptedData = new byte[message.getPayload().remaining()];
//        message.getPayload().get(encryptedData);
//
//        byte[] key = keyManager.getKey(session.getId());
//        byte[] plainData;
//        try {
//            plainData = SM4Util.decrypt(encryptedData, key);
//        } catch (Exception e) {
//            session.close(CloseStatus.BAD_DATA);
//            return;
//        }
//
//        ByteBuf buf = Unpooled.wrappedBuffer(plainData);
//        try {
//            int packetId = PacketCodecUtil.readVarInt(buf);
//            Packet packet = packetRegistry.createPacket(packetId);
//            packet.decode(buf);
//            handlePacket(session, packet);
//        } finally {
//            buf.release();
//        }
//    }
//
//    private void handlePacket(WebSocketSession session, Packet packet) throws Exception {
//
//    }
//
//    public void sendPacket(WebSocketSession session, Packet packet) throws Exception {
//        ByteBuf buf = Unpooled.buffer();
//        try {
//            PacketCodecUtil.writeVarInt(buf, packet.packetId());
//            packet.encode(buf);
//
//            byte[] plainData = new byte[buf.readableBytes()];
//            buf.readBytes(plainData);
//
//            byte[] key = keyManager.getKey(session.getId());
//            byte[] encryptedData = SM4Util.encrypt(plainData, key);
//
//            session.sendMessage(new BinaryMessage(encryptedData));
//        } finally {
//            buf.release();
//        }
//    }
//}
