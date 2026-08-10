package moe.koseirin.nyanruaineo.utils.ErrorUtils;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
public class Error {
    private String error;
    private String message;
    private LocalDateTime timestamp;

}
