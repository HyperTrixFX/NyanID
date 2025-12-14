package moe.koseirin.nyanruaineo.server.V1Controller;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.NyanIdApplication;
import moe.koseirin.nyanruaineo.dto.LoginDTO;
import moe.koseirin.nyanruaineo.dto.RegisterConfirmDTO;
import moe.koseirin.nyanruaineo.dto.RegisterDTO;
import moe.koseirin.nyanruaineo.services.UserServices;
import moe.koseirin.nyanruaineo.utils.EmailHelper.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/zako/v1")
public class UserAuthController {

    private UserServices userServices;
    private EmailService emailService;


    public UserAuthController(UserServices userServices, EmailService emailService) {
        this.userServices = userServices;
        this.emailService = emailService;
    }


    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterDTO registerDTO, HttpServletRequest request, HttpServletResponse response) {
      return userServices.register(registerDTO.getUsername(),registerDTO.getPassword(), registerDTO.getEmail(), registerDTO.getIdempotencyKey(),request,response);
    }

    @PostMapping("/verification")
    public ResponseEntity<?> registerConfirm(@RequestBody RegisterConfirmDTO registerConfirmDTO,HttpServletRequest request){
        return userServices.registerConfirm(registerConfirmDTO.getCode(),request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> Login(@RequestBody LoginDTO loginDTO,HttpServletRequest request) throws Exception {
        return userServices.login(loginDTO.getEmail(), loginDTO.getPassword(), loginDTO.getIdempotencyKey(), request);
    }

    @PostMapping("/2fa")
    public ResponseEntity<?> loginAt2FA(@RequestBody LoginDTO loginDTO,HttpServletRequest request){
        return userServices.l2fa(request, loginDTO.getVerifyCode(), loginDTO.getToken());
    }





    @PostMapping("a")
    public String a(@RequestBody(required = false) JSONObject data) {
//        System.out.println(TurnstileService.validateToken("",TurnstileSecretKey,"").isSuccess());
//        emailService.sendmail("qqqqqqh6@163.com","qqqqqqh6@163.com", EmailBody.RegisterBody.getBody().replace("${link}","https://baidu.com"));
        String E = data.getString("word");
        if (NyanIdApplication.wordBs.contains(E)) {
            E = NyanIdApplication.wordBs.replace(E);
        }
        return E;
    }

}
