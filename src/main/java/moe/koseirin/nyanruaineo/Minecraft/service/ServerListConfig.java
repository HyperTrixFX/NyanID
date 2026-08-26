package moe.koseirin.nyanruaineo.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The sub-server list configuration stored in {@code proxy.backend.servers}:
 *
 * <pre>
 * {
 *   "default_server": "lobby",
 *   "server_list": [
 *     { "uid": "lobby-001",  "priority": 1, "name": "lobby",    "host": "localhost", "port": 25566 },
 *     { "uid": "survival-001", "priority": 2, "name": "survival", "host": "localhost", "port": 25567 }
 *   ]
 * }
 * </pre>
 *
 * A server entry with an empty host falls back to the single {@code proxy.backend.host/port}
 * defaults.
 */
@Data
@NoArgsConstructor
public class ServerListConfig {

    @JSONField(name = "default_server")
    private String defaultServer;

    @JSONField(name = "server_list")
    private List<BackendServer> serverList;
}
