package moe.koseirin.nyanruaineo.utils.ErrUtils;

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
    private int status;
    private  String error;
    private  String message;
    private LocalDateTime timestamp;

}
