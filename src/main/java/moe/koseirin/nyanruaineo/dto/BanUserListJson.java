package moe.koseirin.nyanruaineo.dto;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class BanUserListJson {
    private String BanID;
    private LocalDateTime BanTime;
    private String BannedBy;
    private String reason;
    private boolean active;
    private String uid;
    private int type;

    public BanUserListJson(String banID, LocalDateTime banTime, String bannedBy, String reason, boolean active, String uid, int type) {
        BanID = banID;
        BanTime = banTime;
        BannedBy = bannedBy;
        this.reason = reason;
        this.active = active;
        this.uid = uid;
        this.type = type;
    }

}
