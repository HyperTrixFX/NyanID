package moe.koseirin.nyanruaineo.server.YggdrasilServer.YggdrasilServerJson;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YggdrasilServerJsonMeta {
    private String implementationName;
    private String implementationVersion;
    private String serverName;
    private YggdrasilServerJsonLinks links;
    private boolean feature_non_email_login;

}
