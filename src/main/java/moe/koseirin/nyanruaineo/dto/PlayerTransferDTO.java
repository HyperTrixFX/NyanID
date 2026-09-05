package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;

/*
 * @author KoseiRin_
 * awa
 */

/** 转移玩家到指定子服务器的请求体：玩家用用户名或 UUID，目标服务器用服务器名或 uid。 */
@Getter
@Setter
public class PlayerTransferDTO {
    private String player;
    private String targetServer;
}
