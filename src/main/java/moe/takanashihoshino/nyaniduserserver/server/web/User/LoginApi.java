package moe.takanashihoshino.nyaniduserserver.server.web.User;


import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.GoogleGenerator;
import moe.takanashihoshino.nyaniduserserver.utils.RedisUtils.RedisService;
import moe.takanashihoshino.nyaniduserserver.utils.RsaUtil;
import moe.takanashihoshino.nyaniduserserver.entity.Accounts;
import moe.takanashihoshino.nyaniduserserver.repository.AccountsRepository;
import moe.takanashihoshino.nyaniduserserver.repository.BanUserRepository;
import moe.takanashihoshino.nyaniduserserver.server.web.User.UserJson.LoginJson;
import moe.takanashihoshino.nyaniduserserver.utils.OtherUtils;
import moe.takanashihoshino.nyaniduserserver.repository.UserDevicesRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.UserDevicesService;
import moe.takanashihoshino.nyaniduserserver.entity.UserDevices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("api/zako/v1/login")
public class LoginApi {

    private final RedisService redisService;

    private final AccountsRepository accountsRepository;

    private final UserDevicesService userDevicesService;

    private final UserDevicesRepository userDevicesRepository;

    private final BanUserRepository banUserRepository;

    @Value("${yggdrasil.publicKey}")
    private String  publicKey;

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    @Value("${NyanidSetting.encryptionKey}")
    private String encryptionKey;
    private final Map<String, LoginApi.Const> constMap = new HashMap<>();

    public  String EventID = "LoEvent1";

    public LoginApi(RedisService redisService, AccountsRepository accountsRepository, UserDevicesService userDevicesService, UserDevicesRepository userDevicesRepository, BanUserRepository banUserRepository) {
        this.redisService = redisService;
        this.accountsRepository = accountsRepository;
        this.userDevicesService = userDevicesService;
        this.userDevicesRepository = userDevicesRepository;
        this.banUserRepository = banUserRepository;
    }

    @PostMapping
    public <T> Object PostMethod(@RequestBody(required = false) T data, HttpServletResponse response, HttpServletRequest request) throws Exception {
        if (data != null){
            JSONObject a = JSONObject.parseObject(JSONObject.toJSONString(data));
            String email = a.getString("email");
            String password = a.getString("pwd");
            String DevicesID = a.getString("devid");
            Boolean LoginForWeb = Boolean.valueOf(request.getHeader("LoginForWeb"));
            String DevicesName = a.getString("devname");
            String IP = request.getRemoteAddr();
            if (email != null && password != null){
                JSONObject BanEvent = new JSONObject();
                BanEvent.put(EventID, email);
                    if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")) {
                        return ErrRes.IllegalRequestException("The mailbox is malformed 杂鱼喵~", response);
                    } else {
                        if (accountsRepository.findByEmail(email) != null) {
                            if (redisService.getValue(String.valueOf(BanEvent)) != null && redisService.getValue(String.valueOf(BanEvent)).equals(IP)) {
                                return ErrRes.NotFoundAccountException("The account doesn't exist or is locked because of a password error 杂鱼喵~", response);
                            } else {
                                if (constMap.get(email) == null) {
                                    constMap.put(email, new Const(1));
                                } else if (constMap.get(email).requestCount > 3) {
                                    constMap.remove(email);
                                    redisService.setValueWithExpiration(String.valueOf(BanEvent), IP, 180, TimeUnit.SECONDS);
                                }
                                if (LoginForWeb){
                                    String DevicesIDT = OtherUtils.RandomString(16);
                                    String DevicesNameT = "Web";
                                    DevicesName  = DevicesNameT;
                                    DevicesID = DevicesIDT;
                                }else {
                                    if(DevicesID==null || DevicesName==null) {
                                        return ErrRes.IllegalRequestException("The device ID or device name is missing 杂鱼喵~", response);
                                    }
                                }
                                    String pwd = accountsRepository.LoginByEmail(email);
                                    String lockpwd = OtherUtils.HMACSHA256(encryptionKey,password);
                                    if (Objects.equals(lockpwd, pwd)) {
                                        String uid = accountsRepository.findByEmail(email);
                                        String session = request.getSession().getId();
                                        String  UserSession = userDevicesRepository.findSessionBySession(session);
                                        if (banUserRepository.findBanIDByUid(uid) == null) {
                                            Accounts accounts = accountsRepository.GetUser(uid);
                                            if (accounts.getSecretKey() == null) {
                                                if (Objects.equals(UserSession, session) && OtherUtils.isDaysBefore(userDevicesRepository.findTimeBySession(session), 14)) {
                                                    String token = userDevicesRepository.findTokenBySession(session);
                                                    String clientid = userDevicesRepository.findClientIdByToken(token);
                                                    LoginJson loginJson = new LoginJson();
                                                    loginJson.setData(clientid);
                                                    loginJson.setStatus("success");
                                                    loginJson.setTimestamp(LocalDateTime.now());
                                                    loginJson.setToken(token);
                                                    loginJson.setData(Base64.getEncoder().encodeToString(token.getBytes()));
                                                    userDevicesRepository.UpdateCreateTime(LocalDateTime.now(), token);
                                                    return loginJson;
                                                } else {
                                                    userDevicesRepository.deleteBySession(UserSession);
                                                    String clientid = OtherUtils.RandomString(32);
                                                    String token = OtherUtils.RandomString(64);
                                                    UserDevices userDevices = new UserDevices();
                                                    userDevices.setUid(uid);
                                                    userDevices.setDeviceID(DevicesID);
                                                    userDevices.setDeviceName(DevicesName);
                                                    userDevices.setToken(token);
                                                    userDevices.setIp(IP);
                                                    userDevices.setIsActive(true);
                                                    userDevices.setSession(session);
                                                    userDevices.setClientid(clientid);
                                                    userDevices.setCreateTime(LocalDateTime.now());
                                                    userDevicesService.save(userDevices);
                                                    if (constMap.get(email) != null) {
                                                        constMap.remove(email);
                                                    }
                                                    LoginJson loginJson = new LoginJson();
                                                    loginJson.setData(clientid);
                                                    loginJson.setStatus("success");
                                                    loginJson.setTimestamp(LocalDateTime.now());
                                                    loginJson.setToken(token);
                                                    loginJson.setData(Base64.getEncoder().encodeToString(token.getBytes()));
                                                    return loginJson;
                                                }
                                            } else {
                                                //2fa
                                                String clientid = OtherUtils.RandomString(32);
                                                JSONObject jsonObject = new JSONObject();
                                                jsonObject.put("have2fa",true);
                                                jsonObject.put("Token", RsaUtil.encrypt(clientid,publicKey));
                                                JSONObject object = new JSONObject();
                                                object.put("uid",accounts.getUid());
                                                object.put("skey",accounts.getSecretKey());
                                                redisService.setValueWithExpiration(clientid,JSONObject.toJSONString(object),30,TimeUnit.MINUTES);
                                                return jsonObject;
                                            }
                                        }else {
                                            return ErrRes.NotFoundAccountException("This account has been banned for violating our User Agreement, please create a ticket to appeal ，Your BanID: " + banUserRepository.findBanIDByUid(uid) + "  杂鱼喵~ ", response);
                                        }
                                } else {
                                    if (constMap.get(email) != null) {
                                        constMap.get(email).requestCount++;
                                    }
                                    return ErrRes.NotFoundAccountException("The account doesn't exist or is locked because of a password error 杂鱼喵~", response);
                                }
                            }
                        } else {
                            return ErrRes.NotFoundAccountException("The account doesn't exist or is locked because of a password error 杂鱼喵~", response);
                        }
                    }
            }else {
                return ErrRes.IllegalRequestException("The parameter is incorrect 杂鱼喵~",response);
            }

        }else return ErrRes.IllegalRequestException("The parameter is incorrect 杂鱼喵~",response);
    }



    @PostMapping("2fa")
    public Object verify2FA( HttpServletResponse response, HttpServletRequest request) {
        String code = request.getHeader("verifyCode");
        String Token = request.getHeader("Token");
        if (!code.isEmpty() && !Token.isEmpty()){
            if (code.matches("[0-9]{3,7}")) {
                String DeToken = RsaUtil.decrypt(Token, privateKey);
                if (redisService.getValue(DeToken) != null) {
                    JSONObject value = JSONObject.parseObject(redisService.getValue(DeToken).toString());
                    String uid = value.getString("uid");
                    String skey = value.getString("skey");
                    String IP = request.getRemoteAddr();
                    if (GoogleGenerator.checkCode(skey, Integer.parseInt(code))) {
                        Accounts accounts = accountsRepository.GetUser(uid);
                        String token = OtherUtils.RandomString(64);
                        UserDevices userDevices = new UserDevices();
                        userDevices.setUid(uid);
                        userDevices.setDeviceID(OtherUtils.RandomString(16));
                        userDevices.setDeviceName("Web");
                        userDevices.setToken(token);
                        userDevices.setIp(IP);
                        userDevices.setIsActive(true);
                        userDevices.setSession(request.getSession().getId());
                        userDevices.setClientid(DeToken);
                        userDevices.setCreateTime(LocalDateTime.now());
                        userDevicesService.save(userDevices);
                        if (constMap.get(accounts.getEmail()) != null) {
                            constMap.remove(accounts.getEmail());
                        }
                        LoginJson loginJson = new LoginJson();
                        loginJson.setData(DeToken);
                        loginJson.setStatus("success");
                        loginJson.setTimestamp(LocalDateTime.now());
                        loginJson.setToken(token);
                        loginJson.setData(Base64.getEncoder().encodeToString(token.getBytes()));
                        redisService.deleteValue(DeToken);
                        return loginJson;
                    }else {
                        response.setStatus(403);
                        return null;
                    }
                } else {
                    response.setStatus(403);
                    return null;
                }
            }else {
                response.setStatus(403);
                return null;
            }
        }else {
            response.setStatus(403);
            return null;
        }
    }

    @GetMapping
    public Object GetMethod(HttpServletResponse response){
        return ErrRes.MethodNotAllowedException("Unsupported request patterns 杂鱼喵~",response);
    }


    private static class Const {
        int requestCount;
        Const(int requestCount) {
            this.requestCount = requestCount;
        }
    }
}
