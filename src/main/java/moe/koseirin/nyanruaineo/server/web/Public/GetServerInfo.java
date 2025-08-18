package moe.koseirin.nyanruaineo.server.web.Public;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.OAuthAppRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import static java.lang.Math.random;

@RestController
@RequestMapping("api/zako/v2/server")
public class GetServerInfo {

    private final AccountsRepository accountsRepository;

    private final OAuthAppRepository oAuthAppRepository;

    private final RedisService redisService;

    @Value("${NyanidSetting.msg}")
    private String[] msg;

    public GetServerInfo(AccountsRepository accountsRepository, OAuthAppRepository oAuthAppRepository, RedisService redisService) {
        this.accountsRepository = accountsRepository;
        this.oAuthAppRepository = oAuthAppRepository;
        this.redisService = redisService;
    }

    @GetMapping(produces = "application/json")
    public Object GetServerINFO(HttpServletResponse response) {
        int AllUser = accountsRepository.GetAllUser();
        int GetAllApplication = oAuthAppRepository.GetAllApplication();
        int NumberOfEvents = redisService.getAll();
        JSONObject data = new JSONObject();
        data.put("AllUser",AllUser);
        data.put("msg",msg[(int) (random() * msg.length)]);
        if(redisService.getValue("ServerInfo") != null){
            JSONObject Notification = JSONObject.parseObject(redisService.getValue("ServerInfo").toString());
            data.put("Notification", true);
            data.put("NotificationType", Notification.get("NotificationType"));
            data.put("NotificationData", Notification.get("NotificationData"));
            data.put("NotificationTypeName",Notification.get("NotificationTypeName"));
        }else {
            data.put("Notification", false);
        }
        data.put("AllApplication",GetAllApplication);
        data.put("NumberOfEvents",NumberOfEvents/6);
        response.setStatus(200);
        return data;
    }
}
