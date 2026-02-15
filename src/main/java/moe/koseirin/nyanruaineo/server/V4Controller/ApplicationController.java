package moe.koseirin.nyanruaineo.server.V4Controller;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.services.MicrosoftLogin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("api/zako/v4")
public class ApplicationController {

    private final MicrosoftLogin  microsoftLogin;

    public ApplicationController(MicrosoftLogin microsoftLogin) {
        this.microsoftLogin = microsoftLogin;
    }


    @PostMapping("microsoft/newlogin")
    public ResponseEntity<?> Login(HttpServletResponse response, HttpServletRequest request, @RequestParam(value = "code",required = true) String code, @RequestParam(value = "id_token",required = false) String id_token, @RequestParam(value = "state",required = true) String state) throws IOException {
        return microsoftLogin.MicrosoftLogin(response,request,code,id_token,state);
    }

    @GetMapping("microsoft/newlogin")
    public ResponseEntity<?> redirect(HttpServletResponse response, HttpServletRequest request) throws IOException {
        return microsoftLogin. redirect(response,request);
    }

}
