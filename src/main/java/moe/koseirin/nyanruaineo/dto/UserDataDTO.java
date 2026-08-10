package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/*
 * @author KoseiRin_
 * awa
 */

@Getter
@Setter
@Accessors(chain = true)
public class UserDataDTO {
    private int action;
    private String nickname;
    private String username;
    private String description;
    private String code;

}
