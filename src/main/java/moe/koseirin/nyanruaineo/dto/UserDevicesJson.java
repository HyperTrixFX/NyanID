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
public class UserDevicesJson {

    private String DevicesName;
    private String HWID;
    private String IP;
    private LocalDateTime LoginTime;
    private boolean active;
    private String session;

    public UserDevicesJson(String devicesName, String HWID, String IP,LocalDateTime LoginTime ,boolean active,String session) {
        this.DevicesName = devicesName;
        this.HWID = HWID;
        this.IP = IP;
        this.LoginTime = LoginTime;
        this.active = active;
        this.session = session;
    }


}
