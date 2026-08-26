package moe.koseirin.nyanruaineo.services.impl;

import lombok.RequiredArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerQueryService;
import org.springframework.stereotype.Component;

/*
 * @author KoseiRin_
 * awa
 */
@Component
@RequiredArgsConstructor
public class ProxyFuncImpl {
    private final MinecraftProxy proxy;
    private final PlayerQueryService playerQueryService;



}
