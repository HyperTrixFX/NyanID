package moe.koseirin.nyanruaineo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
@Getter
@Setter
@Accessors(chain = true)
public class ResetPwdDTO {

    private String token;
    private String code;
    private String password;


}
