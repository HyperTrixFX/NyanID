package moe.koseirin.nyanruaineo.server;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletResponse;
//import moe.koseirin.nyanruaineo.websocket.server.BungeeConnectHandle;
import moe.koseirin.nyanruaineo.utils.Respond;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/")
public class MianServer {
    private final Respond respond;

    @Value("${yggdrasil.APILocation}")
    private String  SkinDomains;

    public MianServer(Respond respond) {
        this.respond = respond;
    }

    @PostMapping
    public ResponseEntity<?> PostMethod(HttpServletResponse response){
//        BungeeConnectHandle.sendMessage(JSONObject.toJSONString(sJson));
        return respond.respond(MediaType.APPLICATION_JSON,200,"message","Ok!","timestamp",LocalDateTime.now());
    }
    @GetMapping
    public Object GetMethod(HttpServletResponse response){
        response.setHeader("X-Authlib-Injector-API-Location", "/api/yggdrasil");
//        BungeeConnectHandle.sendMessage(JSONObject.toJSONString(sJson));
        return respond.respond(MediaType.APPLICATION_JSON,200,"message","Ok!","timestamp",LocalDateTime.now());
    }

}
