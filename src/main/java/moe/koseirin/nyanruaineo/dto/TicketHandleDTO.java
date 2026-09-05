package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;

/*
 * @author KoseiRin_
 * awa
 */

/** 管理员处理工单的请求体：更新状态，可选指派处理人、填写回复。 */
@Getter
@Setter
public class TicketHandleDTO {
    private String status;
    private String handlerUid;
    private String reply;
}
