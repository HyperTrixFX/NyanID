package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.BanUserList;
import moe.koseirin.nyanruaineo.entity.UserDevices;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.SqlService.UserDevicesService;
import moe.koseirin.nyanruaineo.utils.WebMvc.StrictIpResolver;
import moe.koseirin.nyanruaineo.utils.utilset;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
    @Value("${yggdrasil.publicKey}")
    private String  publicKey;

    private final AccountsRepository accountsRepository;
    private final StrictIpResolver strictIpResolver;
    private final NyanIDuserRepository nyanIDuserRepository;
    private final UserDevicesService userDevicesService;
    private final BanUserRepository banUserRepository;
    private final RedisService redisService;
    private final Respond respond;
    private final utilset utilset;

    public MicrosoftLogin(AccountsRepository accountsRepository, Respond respond, utilset utilset, StrictIpResolver strictIpResolver, NyanIDuserRepository nyanIDuserRepository, UserDevicesService userDevicesService, BanUserRepository banUserRepository, RedisService redisService) {
        this.accountsRepository = accountsRepository;
        this.respond = respond;
        this.utilset = utilset;
        this.strictIpResolver = strictIpResolver;
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.userDevicesService = userDevicesService;
        this.banUserRepository = banUserRepository;
        this.redisService = redisService;
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
                .add("scope", "user.read")
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
        JSONObject json = null;
        if (respon.body() != null) {
            json = JSONObject.parseObject(respon.body().string());
        }
        //
        Request req1 = null;
        if (json != null) {
            req1 = new Request.Builder()
                    .url("https://graph.microsoft.com/v1.0/me")
                    .header("Authorization", "Bearer "+ json.getString("access_token")).build();
        }
        Response respon1 = null;
        if (req1 != null) {
            respon1 = client.newCall(req1).execute();
        }
        if (respon1 != null && !respon1.isSuccessful()) {
            response.sendRedirect(HOST + "/#/login?error=401");
        }
        //
        JSONObject USER = null;
        if (respon1 != null && respon1.body() != null) {
            USER = JSONObject.parseObject(respon1.body().string());
        }
        String ocid = null;
        if (USER != null) {
            ocid = USER.getString("id");
        }
        if (USER != null) {
            String nickname = USER.getString("surname")+USER.getString("givenName");
        }
        if (USER != null) {
            String mail = USER.getString("mail");
        }
        Accounts accounts = accountsRepository.GetMicrosoftUser(ocid);
        if (accounts == null){
            response.sendRedirect(HOST+"/#/login?error=4011");
//            return ResponseEntity.ok(USER);
        }
        // Check if user is banned
        BanUserList banUserList = banUserRepository.LEVE450TRUE(accounts.getUid());
        if (banUserList != null) {
            return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "异常等级LEVEL" + banUserList.getType() +
                            ",异常原因" + banUserList.getReason() + ",处罚ID" + banUserList.getBanID() + "账户状态异常杂鱼喵!",
                    "timestamp", LocalDateTime.now());
        }
        // Handle 2FA authentication
        if (accounts.getSecretKey() != null) {
            String eid = utilset.RandomString(32);
            JSONObject object = new JSONObject();
            object.put("uid", accounts.getUid());
            object.put("skey", accounts.getSecretKey());
            redisService.setValueWithExpiration(eid, JSONObject.toJSONString(object), 10, TimeUnit.MINUTES);
            return respond.respond(MediaType.APPLICATION_JSON, 200, "have2fa", true, "Token", utilset.encrypt(eid, publicKey));
        }




        String session = utilset.GetSessionUUID(request, accounts.getUid());
        // Generate a random client ID and token for the new session
        String clientid = utilset.RandomString(32);
        String token = utilset.RandomString(64);
        // Create a new UserDevices object to store device information
        UserDevices userDevices = new UserDevices();
        userDevices.setUid(accounts.getUid());
        userDevices.setDeviceID("null");
        userDevices.setDeviceName("Microsoft");
        userDevices.setToken(token);
        userDevices.setIp(strictIpResolver.getStrictClientIp(request));
        userDevices.setIsActive(true);
        userDevices.setSession(session);
        userDevices.setClientId(clientid);
        userDevices.setHardwareID(null);
        userDevices.setCreateTime(LocalDateTime.now());
        // Save the new device information to the database
        userDevicesService.save(userDevices);
        if (respon1 != null) {
            respon1.close();
        }
        respon.close();
        Cookie cookie = new Cookie("access_token", utilset.encrypt(token, publicKey));
        cookie.setPath("/");
        cookie.setDomain(HOST.replace("http://", "").replace("https://", "").replaceAll(":\\d+", "").replace("/", ""));
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
        response.sendRedirect(HOST + "/#/");
        return null;
//        return respond.respond(MediaType.APPLICATION_JSON, 200, "have2fa", false, "access_token", utilset.encrypt(token, publicKey), "timestamp",LocalDateTime.now());
//        return respond.respond(MediaType.APPLICATION_JSON, 200,"respon_s",JSONObject.parseObject(respon1.body().string()));
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
        if (respon.body() != null) {
            json = JSONObject.parseObject(respon.body().string());
        }
        return json;
    }
}
