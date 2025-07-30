package moe.takanashihoshino.nyaniduserserver.server.web.User;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.server.web.User.UserJson.BanUserListJson;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.repository.UserDevicesRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.impl.BanUserServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/zako/v1/user/violation/history")
public class ViolationHistoryApi {


    private final UserDevicesRepository userDevicesRepository;


    private final BanUserServiceImpl banUserService;

    public ViolationHistoryApi(UserDevicesRepository userDevicesRepository, BanUserServiceImpl banUserService) {
        this.userDevicesRepository = userDevicesRepository;
        this.banUserService = banUserService;
    }

    @GetMapping(produces = "application/json")
    public  Object Get(HttpServletResponse response, HttpServletRequest request) {
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        List<BanUserListJson> result = banUserService.GetBanList(uid);
        if (result.isEmpty()){
            response.setStatus(404);
            return ErrRes.NotFoundAccountException("Null",response);
        }else {
            return result;
        }

    }


}
