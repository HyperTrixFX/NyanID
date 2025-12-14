package moe.koseirin.nyanruaineo.server.V2Controller;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.dto.UserResponseDTO;
import moe.koseirin.nyanruaineo.services.UserInfoServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("searchuser")
    public Object SearchUserApi(@RequestBody UserResponseDTO userResponseDTO) {
        return userInfoServices.SearchUserApi(userResponseDTO);
    }

}
