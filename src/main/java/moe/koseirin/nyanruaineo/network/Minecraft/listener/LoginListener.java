package moe.koseirin.nyanruaineo.network.Minecraft.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PacketReceivedEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.EncryptionResponsePacket;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginStartPacket;
import moe.koseirin.nyanruaineo.network.Minecraft.service.LoginService;
import org.springframework.stereotype.Component;

/*
 * @author KoseiRin_
 * awa
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginListener {


    private final LoginService loginService;

    @EventHeader
    public void onLoginStart(PacketReceivedEvent event) {
        if (event.getPacket() instanceof LoginStartPacket) {
            loginService.startLogin(event.getCtx().channel(), (LoginStartPacket) event.getPacket());
        }
    }

    @EventHeader
    public void onEncryptionResponse(PacketReceivedEvent event) {
        if (event.getPacket() instanceof EncryptionResponsePacket) {
            loginService.completeLogin(event.getCtx().channel(), (EncryptionResponsePacket) event.getPacket());
        }
    }

}

