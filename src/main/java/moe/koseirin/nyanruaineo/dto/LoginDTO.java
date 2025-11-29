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
public class LoginDTO {
    
    private String email;
    private String password;
    private String idempotencyKey;

    //
    private boolean have2fa;
    private String token;

    //
    private String verifyCode;

}
