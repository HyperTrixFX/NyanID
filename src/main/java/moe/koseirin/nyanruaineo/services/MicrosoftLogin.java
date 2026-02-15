package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.WebMvc.StrictIpResolver;
import moe.koseirin.nyanruaineo.utils.utilset;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;

/*
 * @author KoseiRin_
 * awa
 */
@Service
public class MicrosoftLogin {

    @Value("${NyanidSetting.HOST}")
    private String HOST;
    @Value("${NyanidSetting.MicrosoftLogin.appid}")
    private String appid;
    @Value("${NyanidSetting.MicrosoftLogin.redirect_uri}")
    private String redirect_uri;
    @Value("${NyanidSetting.MicrosoftLogin.secretkey}")
    private String secretkey;

    private final AccountsRepository accountsRepository;
    private final Respond respond;
    private final utilset utilset;
    private final StrictIpResolver strictIpResolver;

    public MicrosoftLogin(AccountsRepository accountsRepository, Respond respond, utilset utilset, StrictIpResolver strictIpResolver) {
        this.accountsRepository = accountsRepository;
        this.respond = respond;
        this.utilset = utilset;
        this.strictIpResolver = strictIpResolver;
    }

    public ResponseEntity<?> redirect(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String SID = utilset.GetSessionUUID(request,strictIpResolver.getStrictClientIp(request));
        String authorization_endpoint = PAPI().getString("authorization_endpoint");
        String url = authorization_endpoint+"?client_id="+appid+"&prompt=consent&redirect_uri="+redirect_uri+"&response_type=code+id_token&scope=user.read+openid+profile+email&response_mode=form_post&state="+SID+"&nonce="+utilset.RandomString(8);
        return respond.respond(MediaType.APPLICATION_JSON, 200,"u",url);
    }

    public ResponseEntity<?> MicrosoftLogin(HttpServletResponse response,HttpServletRequest request, String code, String id_token, String state) throws IOException {
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("client_id", appid)
                .add("scope", "openid")
                .add("code", code)
                .add("redirect_uri", redirect_uri)
                .add("grant_type", "authorization_code")
                .add("client_secret", secretkey)
                .build();
        Request req = new Request.Builder()
                .url(PAPI().getString("token_endpoint"))
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response respon = client.newCall(req).execute();
        if(!respon.isSuccessful()){
            response.sendRedirect(HOST+"/#/login?error=401");
        }
        JSONObject json = JSONObject.parseObject(respon.body().string());

        return respond.respond(MediaType.APPLICATION_JSON, 200,"respon_s",json);


        //return null;
        //        response.sendRedirect(HOST+"/login?access_token="+access_token);

    }


    private Object Register() {


        return null;
    }


    private Object Login() {




        return null;
    }



    private JSONObject PAPI() throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url("https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration")
                .build();
        Response respon = client.newCall(req).execute();
        JSONObject json = new JSONObject();
        if(!respon.isSuccessful()){
            json.put("message","An error occurred while accessing the Microsoft API.");
            json.put("timestamp", LocalDateTime.now());
        }
        json = JSONObject.parseObject(respon.body().string());
        return json;
    }
}
