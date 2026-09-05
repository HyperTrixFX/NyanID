package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;

/*
 * @author KoseiRin_
 * awa
 */

/** 用户提交工单的请求体。 */
@Getter
@Setter
public class TicketCreateDTO {
    private Integer type;
    private String description;
}
