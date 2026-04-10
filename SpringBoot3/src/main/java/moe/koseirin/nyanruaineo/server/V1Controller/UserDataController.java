package moe.koseirin.nyanruaineo.server.V1Controller;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.dto.ResetPwdDTO;
import moe.koseirin.nyanruaineo.dto.UserDataDTO;
import moe.koseirin.nyanruaineo.services.UserDataServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    @PostMapping("userdata")
    public ResponseEntity<?> ActionMethod(@RequestBody UserDataDTO userDataDTO, HttpServletRequest request){
        return userDataServices.ActionMethod(request,userDataDTO.getAction(),userDataDTO.getNickname(),userDataDTO.getUsername(),userDataDTO.getDescription(),userDataDTO.getCode());
    }


    @PutMapping("userdata")
    public ResponseEntity<?> PutMethod(@RequestParam(value = "avatar", required = true) MultipartFile avatar, HttpServletRequest request, HttpServletResponse response) throws IOException {
        return userDataServices.PutMethod(avatar,request,response);


    }
}
