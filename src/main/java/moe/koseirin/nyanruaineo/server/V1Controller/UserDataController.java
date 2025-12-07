package moe.koseirin.nyanruaineo.server.V1Controller;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.dto.ResetPwdDTO;
import moe.koseirin.nyanruaineo.services.UserDataServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/zako/v1")
public class UserDataController {

    private final UserDataServices userDataServices;

    public UserDataController(UserDataServices userDataServices) {
        this.userDataServices = userDataServices;
    }

    @PostMapping("devices")
    public ResponseEntity<String> userinfo(){
        return null;
    }

    @GetMapping("forgetpwd")
    public ResponseEntity<?> ForgetPwdStep1(@RequestParam(value = "email" ,required = false) String email, HttpServletRequest request){
        return userDataServices.ForgetPwdStep1(email,request);
    }

    @PostMapping("resetpwd")
    public ResponseEntity<?> ForgetPwdStep2(@RequestBody ResetPwdDTO resetPwdDTO, HttpServletRequest request){
        return userDataServices.ForgetPwdStep2(resetPwdDTO.getToken(),resetPwdDTO.getCode(),resetPwdDTO.getPassword(),request);
    }
}
