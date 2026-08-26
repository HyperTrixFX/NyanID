package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A target backend (sub) server the proxy can route a player to, as stored in the
 * {@code proxy.backend.servers} configuration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackendServer {

    private String uid;
    private int priority;
    private String name;
    private String host;
    private int port;
}
