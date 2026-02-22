package moe.koseirin.nyanruaineo.utils.WebMvc;

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
public class TurnstileResponse {
    private boolean success;
    private String challenge_ts;
    private String hostname;
    private String action;
    private String cdata;
}
