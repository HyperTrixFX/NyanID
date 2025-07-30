package moe.takanashihoshino.nyaniduserserver.server;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.SJson;
import moe.takanashihoshino.nyaniduserserver.utils.OtherUtils;
import moe.takanashihoshino.nyaniduserserver.websocket.server.BungeeConnectHandle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/")
public class MianServer {
    @Value("${yggdrasil.APILocation}")
    private String  SkinDomains;

    @PostMapping
    public Object PostMethod(HttpServletResponse response){
        response.setHeader("NekoServer-ABYDOS-NYANID", OtherUtils.RandomString(10));
        SJson sJson = new SJson();
        sJson.setStatus(200);
        sJson.setMessage("Ok!");
        sJson.setTimestamp(LocalDateTime.now());
        BungeeConnectHandle.sendMessage(JSONObject.toJSONString(sJson));
        return  sJson;
    }
    @GetMapping
    public Object GetMethod(HttpServletResponse response){
        response.setHeader("NekoServer-ABYDOS-NYANID", OtherUtils.RandomString(10));
        response.setHeader("X-Authlib-Injector-API-Location", "/api/yggdrasil");
        SJson sJson = new SJson();
        sJson.setStatus(200);
        sJson.setMessage("Ok!");
        sJson.setTimestamp(LocalDateTime.now());
        BungeeConnectHandle.sendMessage(JSONObject.toJSONString(sJson));
        return  sJson;
    }
}
