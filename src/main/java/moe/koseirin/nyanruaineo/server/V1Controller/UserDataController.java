package moe.koseirin.nyanruaineo.server.V1Controller;

/*
 * @author KoseiRin_
 * awa
 */

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/zako/v1")
public class UserDataController {

    @PostMapping("devices")
    public ResponseEntity<String> userinfo(){
        return null;
    }
}
