package moe.koseirin.nyanruaineo.server.YggdrasilServer.YggdrasilServerJson;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class YggdrasilServerJsonRoot {
    private YggdrasilServerJsonMeta meta;
    private String[] skinDomains;
    private String signaturePublickey;

}
