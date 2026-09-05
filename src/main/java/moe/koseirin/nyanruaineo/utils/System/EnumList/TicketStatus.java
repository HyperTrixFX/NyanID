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

    /** 是否已结束（终态）：通过 / 拒绝 / 关闭。未结束 = 待处理 / 处理中。 */
    public boolean isFinished() {
        return this == APPROVED || this == REJECTED || this == CLOSED;
    }
}
