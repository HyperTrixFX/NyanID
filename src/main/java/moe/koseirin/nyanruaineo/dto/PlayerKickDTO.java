package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;

/*
 * @author KoseiRin_
 * awa
 */

/** 踢出玩家的请求体：玩家用用户名或 UUID，reason 可选。 */
@Getter
@Setter
public class PlayerKickDTO {
    private String player;
    private String reason;
}
