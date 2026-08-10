package moe.koseirin.nyanruaineo.server.V2Controller;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.services.ServerServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/zako/v2")
public class ServerInfoController {

    private final ServerServices services;

    public ServerInfoController(ServerServices services) {
        this.services = services;
    }


    @GetMapping(path = "server",produces = "application/json")
    public ResponseEntity<?> info(HttpServletRequest request) {
        return services.info(request);

    }
}
