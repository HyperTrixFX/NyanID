package moe.koseirin.nyanruaineo.websocket.packet;


/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class S32 {
    public String packet = "S32";

    public Boolean bind;

    public String uuid;

    public String muid;

    public String username;


}
