package moe.koseirin.nyanruaineo.server.V1Controller;


import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.services.UserInfoServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/zako/v1")
public class UserInfoController {
    private final UserInfoServices userInfoServices;

    public UserInfoController(UserInfoServices userInfoServices) {
        this.userInfoServices = userInfoServices;
    }

    @GetMapping("userinfo")
    public ResponseEntity<?> GetUserInfo(HttpServletRequest request){
        return userInfoServices.getUserInfo(request);
    }

    @GetMapping("user/violation/history")
    public ResponseEntity<?> GetUserViolationHistory(HttpServletRequest request){
        return userInfoServices.ViolationHistory(request);
    }
}
