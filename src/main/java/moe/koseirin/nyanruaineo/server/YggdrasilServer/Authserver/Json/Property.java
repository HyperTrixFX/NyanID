package moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver.Json;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Property {

    private String name;
    private String value;
    // 可选字段
    private String signature;

    public Property() {}

    public Property(String name, String value, String signature) {
        this.name = name;
        this.value = value;
        this.signature = signature;
    }


}
