package moe.koseirin.nyanruaineo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 后端服务器列表项：在 {@code BackendServer} 静态配置之上，附加运行时状态
 * （是否在线、在线人数、连接的玩家列表）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackendServerStatusDTO {

    private String uid;
    private int priority;
    private String name;
    private String host;
    private int port;
    /** 后端是否在线 */
    private boolean online;
    /** 当前连接到该后端的玩家数量 */
    private int onlineCount;
    /** 当前连接到该后端的玩家列表 */
    private List<PlayerEntry> players;

    /** 一名已连接玩家的最小信息 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerEntry {
        private String username;
        private UUID uuid;
    }
}
