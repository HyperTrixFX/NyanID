package moe.koseirin.nyanruaineo.server.web.Admin;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.utils.ErrUtils.ErrRes;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.repository.UserPermissionsRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v3/zako/administration/validate")
public class Validate {

    private final UserPermissionsRepository userPermissionsRepository;

    private final UserDevicesRepository userDevicesRepository;

    public Validate(UserPermissionsRepository userPermissionsRepository, UserDevicesRepository userDevicesRepository) {
        this.userPermissionsRepository = userPermissionsRepository;
        this.userDevicesRepository = userDevicesRepository;
    }

    @PostMapping
    public <T> Object Validate(@RequestBody(required = false) T data, HttpServletResponse response, HttpServletRequest request){
        if (data != null) {
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
            if (json.containsKey("action") && json.containsKey("key")){
                String Authorization = request.getHeader("Authorization");
                String Token = Authorization.replace("Bearer ", "").replace(" ", "");
                String uid = userDevicesRepository.findUidByToken(Token);
                String action = json.getString("action");
                String key = json.getString("key");
                if (userPermissionsRepository.getUserGroup(uid,action,key) != null){
                    response.setHeader("UserGroup",userPermissionsRepository.getUserGroup(uid,action,key));
                    response.setStatus(204);
                    return null;
                }else {
                    return ErrRes.MethodNotAllowedException("MethodNotAllowed",response);
                }
            }else {
                return ErrRes.MethodNotAllowedException("MethodNotAllowed",response);
            }
        }else {
            return ErrRes.MethodNotAllowedException("MethodNotAllowed",response);
        }
    }

    @GetMapping
    public Object Object(HttpServletResponse response){
        return ErrRes.MethodNotAllowedException("MethodNotAllowed",response);
    }

}
