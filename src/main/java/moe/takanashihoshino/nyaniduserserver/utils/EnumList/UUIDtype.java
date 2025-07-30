package moe.takanashihoshino.nyaniduserserver.utils.EnumList;

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
