package moe.koseirin.nyanruaineo.utils.ErrUtils.Ygg;


/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private String error;
    private String errorMessage;
    private String cause;

}