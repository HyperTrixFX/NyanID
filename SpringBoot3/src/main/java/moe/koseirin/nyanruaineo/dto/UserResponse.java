package moe.koseirin.nyanruaineo.dto;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private String value;
    private String uid;

    public UserResponse(String value, String link) {
        this.value = value;
        this.uid = link;
    }
}
