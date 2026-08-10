package moe.koseirin.nyanruaineo.utils.ErrorUtils;


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

    public ErrorResponse(String error, String errorMessage, String cause) {
        this.error = error;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }
}