package moe.koseirin.nyanruaineo.network.Minecraft.event;

import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.DisconnectPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.service.LoginService;
import moe.koseirin.nyanruaineo.network.Minecraft.util.PacketSender;
import org.springframework.stereotype.Component;

/*
 * @author KoseiRin_
 * awa
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLoginHandler {
    private final LoginService loginService;
    private final PacketSender packetSender;

    @EventHeader
    public void onPlayerAuthenticated(PlayerAuthenticatedEvent event) {
        String username = event.getUsername();
        log.info("Player {} authenticated, executing custom logic...", username);
        JSONObject J = new JSONObject();
        J.put("text", "Hello," + username);
        String reason = JSONObject.toJSONString(J);
        DisconnectPacket disconnect = new DisconnectPacket(reason);
        event.getChannel().writeAndFlush(disconnect).addListener(future -> {
            if (future.isSuccess()) {
                log.info("Disconnect packet sent successfully");
            } else {
                log.error("Failed to send disconnect packet", future.cause());
            }

        });


        // 继续登录流程
//        loginService.continueLogin(event.getChannel(), username, event.getUuid());
    }



}
