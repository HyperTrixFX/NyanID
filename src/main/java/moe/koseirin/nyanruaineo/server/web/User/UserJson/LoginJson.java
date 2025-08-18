package moe.koseirin.nyanruaineo.server.web.User.UserJson;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoginJson {
    private String data;

    private String status;

    private String token;

    private LocalDateTime timestamp;

}
