package moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver.Json;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;



import java.util.List;

@Getter
@Setter
public class CharacterInformationJson {

    private String id;

    private String name;

    private List<Property> properties;

    public CharacterInformationJson() {}

    // 带参构造器
    public CharacterInformationJson(String id, String name, List<Property> properties) {
        this.id = id;
        this.name = name;
        this.properties = properties;
    }


}
