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
public class RegisterConfirmDTO {
    private String code;
}
