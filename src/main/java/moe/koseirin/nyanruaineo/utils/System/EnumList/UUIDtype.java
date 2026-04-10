package moe.koseirin.nyanruaineo.utils.System.EnumList;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(chain=true)
public enum UUIDtype{
    NyanID("NyanIDUser"),
    Yggdrasil("OfflinePlayer"),
    Normal("");

    private final String name;

    UUIDtype(String name){
        this.name = name;
    }

    public String getType() {
        return name;
    }
}
