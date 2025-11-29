package moe.koseirin.nyanruaineo.server.web.User;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.server.web.User.UserJson.UserDevicesJson;
import moe.koseirin.nyanruaineo.utils.ErrUtils.ErrRes;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.impl.UserDevicesServiceImpl;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/zako/v1/user/devices")
public class UserDevices {


    private final UserDevicesServiceImpl userDevicesService;

    private final UserDevicesRepository userDevicesRepository;

    private final utilset utilset;

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    public UserDevices(UserDevicesServiceImpl userDevicesService, UserDevicesRepository userDevicesRepository, utilset utilset) {
        this.userDevicesService = userDevicesService;
        this.userDevicesRepository = userDevicesRepository;
        this.utilset = utilset;
    }


    @GetMapping(produces = "application/json")
    public  Object SearchUserApi(HttpServletResponse response,HttpServletRequest request){
        String Authorization = request.getHeader("Authorization");
        String raw = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(raw,privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
            List<UserDevicesJson> result = userDevicesService.GetDevices(uid);
            if (result.isEmpty()){
                response.setStatus(404);
                return ErrRes.NotFoundAccountException("Null",response);
            }else {
                return result;
            }
        }

    @DeleteMapping(produces = "application/json")
    public <T> Object DeleteDevices(@RequestBody(required = false) T data,HttpServletResponse response, HttpServletRequest request){
        if (data != null) {
            JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(data));
            String value = jsonObject.getString("value");
            userDevicesRepository.deleteBySession(value);
            response.setStatus(204);
            return null;
        }else {
            return ErrRes.NotFoundAccountException("null",response);
        }
    }
}
