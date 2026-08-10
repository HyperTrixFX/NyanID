package moe.koseirin.nyanruaineo.websocket.event;

/*
 * @author KoseiRin_
 * awa
 */


import moe.koseirin.nyanruaineo.network.Interface.Packet;
import org.springframework.web.socket.WebSocketSession;

public record PacketReceivedEvent(WebSocketSession session, Packet packet) {

}