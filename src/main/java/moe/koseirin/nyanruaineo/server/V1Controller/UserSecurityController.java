package moe.koseirin.nyanruaineo.server.V1Controller;

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.dto.DeleteDevicesDTO;
import moe.koseirin.nyanruaineo.services.UserSecurityServices;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("user/devices")
    public ResponseEntity<?> GetDevices(HttpServletRequest request) {
        return userSecurityServices.GetDevices(request);
    }

    @DeleteMapping("user/devices")
    public ResponseEntity<?> DeleteDevices(@RequestBody DeleteDevicesDTO deleteDevicesDTO) {
        return userSecurityServices.DeleteDevices(deleteDevicesDTO.getValue());
    }






}
