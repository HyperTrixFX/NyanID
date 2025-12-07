package moe.koseirin.nyanruaineo.server.V2Controller;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.services.UserInfoServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/zako/v2")
public class PublicUserInfoController {

    private final UserInfoServices userInfoServices;

    public PublicUserInfoController(UserInfoServices userInfoServices) {
        this.userInfoServices = userInfoServices;
    }


    @GetMapping("userinfo/{uuid}")
    public ResponseEntity<?> getUserInfo(@PathVariable("uuid") String uuid, HttpServletRequest request) {
            return userInfoServices.getUserInfo(uuid,request);
    }
}
