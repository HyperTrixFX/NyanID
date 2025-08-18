package moe.koseirin.nyanruaineo.server.web.Public;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.utils.ErrUtils.ErrRes;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.impl.UserServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/zako/v2/searchuser")
public class SearchUser {

    private final UserServiceImpl userService;

    public SearchUser(UserServiceImpl userService) {
        this.userService = userService;
    }

    @PostMapping(produces = "application/json")
    public <T> Object SearchUserApi(@RequestBody(required = false) T data, HttpServletResponse response, HttpServletRequest request){
        if (data != null) {
            JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(data));
            String value = jsonObject.getString("value");
            List<UserResponse> result = userService.searchUsers(value);
            if (result.isEmpty()){
                response.setStatus(404);
                return ErrRes.NotFoundAccountException("Null",response);
            }else {
                return result;
            }

        }else {
            return ErrRes.NotFoundAccountException("null",response);
        }
    }
}
