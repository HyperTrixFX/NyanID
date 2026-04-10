package moe.koseirin.nyanruaineo.server.web.User;


/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.server.web.User.UserJson.BanUserListJson;
import moe.koseirin.nyanruaineo.utils.ErrUtils.ErrRes;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.impl.BanUserServiceImpl;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/zako/v1/user/violation/history")
public class ViolationHistoryApi {
    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    private final UserDevicesRepository userDevicesRepository;
    private final utilset utilset;


    private final BanUserServiceImpl banUserService;

    public ViolationHistoryApi(UserDevicesRepository userDevicesRepository, utilset utilset, BanUserServiceImpl banUserService) {
        this.userDevicesRepository = userDevicesRepository;
        this.utilset = utilset;
        this.banUserService = banUserService;
    }

    @GetMapping(produces = "application/json")
    public Object Get(HttpServletResponse response, HttpServletRequest request) {
        String Authorization = request.getHeader("Authorization");
        String raw = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(raw,privateKey);
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
