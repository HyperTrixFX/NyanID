package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.lang.Math.random;

/*
 * @author KoseiRin_
 * awa
 */
@Service
public class ServerServices {


    private final RedisService redisService;

    private final utilset utilset;

    private final Respond respond;

    @Value("${NyanidSetting.msg}")
    private String[] msg;

    @Value("${NyanidSetting.EnableUserRegister}")
    private boolean EnableUserRegister;

    @Value("${yggdrasil.publicKey}")
    private String  publicKey;

    @Value("${NyanidSetting.TurnstileSecretSiteKey}")
    private String TurnstileSecretSiteKey;

    public ServerServices(RedisService redisService, utilset utilset, Respond respond) {
        this.redisService = redisService;
        this.utilset = utilset;
        this.respond = respond;
    }

    @Transactional
    public ResponseEntity<?> info(HttpServletRequest request){
        String message = msg[(int) (random() * msg.length)];
        Object[] responseParams;

        if (redisService.getValue("ServerInfo") != null) {
            JSONObject notification = JSONObject.parseObject(redisService.getValue("ServerInfo").toString());
            responseParams = new Object[]{
                    "TurnstileSecretSiteKey", TurnstileSecretSiteKey,
                    "EnableUserRegister", EnableUserRegister,
                    "msg", message,
                    "Notification", true,
                    "NotificationType", notification.get("NotificationType"),
                    "NotificationData", notification.get("NotificationData"),
                    "NotificationTypeName", notification.get("NotificationTypeName"),
                    "publicKey",publicKey
            };
        } else {
            responseParams = new Object[]{
                    "TurnstileSecretSiteKey", TurnstileSecretSiteKey,
                    "EnableUserRegister", EnableUserRegister,
                    "msg", message,
                    "Notification", false,
                    "publicKey", publicKey
            };
        }

        return respond.respond(MediaType.APPLICATION_JSON, 200, responseParams);




    }
}
