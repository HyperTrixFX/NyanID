package moe.koseirin.nyanruaineo.server.V1Controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.services.UserSecurityServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
 * @author KoseiRin_
 * awa
 */
@RestController
@RequestMapping("api/zako/v1")
public class UserSecurityController {

    private final UserSecurityServices userSecurityServices;


    public UserSecurityController(UserSecurityServices userSecurityServices) {
        this.userSecurityServices = userSecurityServices;
    }


    @PostMapping("user/2fa/open2fa")
    public ResponseEntity<?> Open2fa(HttpServletRequest request){
        return userSecurityServices.Open2FA(request);
    }

    @PostMapping("user/2fa/close2fa")
    public ResponseEntity<?> Close2fa(HttpServletRequest request){
        return userSecurityServices.Close2FA(request);
    }









}
