package moe.koseirin.nyanruaineo.utils.System.EnumList;

import lombok.Getter;

/*
 * @author KoseiRin_
 * awa
 */

@Getter
public enum TicketStatus {
    PENDING(0),
    PROCESSING(1),
    APPROVED(2),
    REJECTED(-1),
    CLOSED(3);

    private final int code;

    TicketStatus(int code) {
        this.code = code;
    }
}
