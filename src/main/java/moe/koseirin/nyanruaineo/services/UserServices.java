package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.NyanIdApplication;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.BanUserList;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.entity.UserDevices;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.utils.EmailHelper.EmailService;
import moe.koseirin.nyanruaineo.utils.EnumList.EmailBody;
import moe.koseirin.nyanruaineo.utils.EnumList.UUIDtype;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.SqlService.NyanidUserService;
import moe.koseirin.nyanruaineo.utils.SqlService.UserDevicesService;
import moe.koseirin.nyanruaineo.utils.SqlService.UserService;
import moe.koseirin.nyanruaineo.utils.WebMvc.*;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/*
 * @author KoseiRin_
 * awa
 */


@Service
public class UserServices {

    private final AccountsRepository accountsRepository;

    private final EmailService emailService;

    private final RedisService redisService;

    private final UserService userService;

    private final NyanidUserService nyanidUserService;

    private final BanUserRepository banUserRepository;

    private final UserDevicesRepository userDevicesRepository;

    private final UserDevicesService userDevicesService;

    private final StrictIpResolver strictIpResolver;

    private final utilset utilset;
    
    private final Respond respond;

    @Value("${NyanidSetting.encryptionKey}")
    private String encryptionKey;

    @Value("${NyanidSetting.TurnstileSecretKey}")
    private String TurnstileSecretKey;

    @Value("${NyanidSetting.HOST}")
    private String HOST;

    @Value("${yggdrasil.publicKey}")
    private String  publicKey;

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    @Value("${NyanidSetting.EnableUserRegister}")
    private boolean EnableUserRegister;
    public String EventID = "RegEvent1";
    public  String EventID2 = "LoEvent1";

    private final Map<String, UserServices.Const> constMap = new HashMap<>();


    public UserServices(AccountsRepository accountsRepository, EmailService emailService, RedisService redisService, UserService userService, NyanidUserService nyanidUserService, BanUserRepository banUserRepository, UserDevicesRepository userDevicesRepository, UserDevicesService userDevicesService, StrictIpResolver strictIpResolver, utilset utilset, Respond respond) {
        this.accountsRepository = accountsRepository;
        this.emailService = emailService;
        this.redisService = redisService;
        this.userService = userService;
        this.nyanidUserService = nyanidUserService;
        this.banUserRepository = banUserRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.userDevicesService = userDevicesService;
        this.strictIpResolver = strictIpResolver;
        this.utilset = utilset;
        this.respond = respond;
    }


    @Transactional
    public ResponseEntity<?> register(String username, String password, String email,String idempotencyKey, HttpServletRequest request, HttpServletResponse response) {
        if (!EnableUserRegister) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","服务器未开启用户注册功能喵!","timestamp", LocalDateTime.now());
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty() || idempotencyKey == null || idempotencyKey.isEmpty()) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","非法参数喵!","timestamp", LocalDateTime.now());
        }

        if(!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}") ){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","邮箱格式错误喵!","timestamp", LocalDateTime.now());
        }

        if (!username.matches("(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,20}")){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","用户名只允许英文,数字和下划线组合喵!","timestamp", LocalDateTime.now());
        }

        if (NyanIdApplication.wordBs.contains(username)) {
            redisService.setValueWithExpiration(strictIpResolver.getStrictClientIp(request),1,1, TimeUnit.MINUTES);
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","非法用户名,请先仔细阅读我们的服务条款再进行注册喵!","timestamp", LocalDateTime.now());
        }

        if (accountsRepository.findByEmail(email) == null && accountsRepository.GetUser(username) == null) {
            JSONObject Event = new JSONObject();
            Event.put(EventID,email);
            if (redisService.getValue(String.valueOf(Event)) != null) {
                return respond.respond(MediaType.APPLICATION_JSON,403, "message","该邮箱已被暂时冻结注册喵!","timestamp", LocalDateTime.now());
            }
            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,20}$")){
                return respond.respond(MediaType.APPLICATION_JSON,403, "message","您的密码至少包含一个大写字母(A-Z),包含一个小写字母(a-z),包含一个数字(0-9),包含一个特殊字符,且长度至少为 9 个字符喵!","timestamp", LocalDateTime.now());
            }
            TurnstileResponse turnstileResponse = TurnstileService.validateToken(idempotencyKey,TurnstileSecretKey,strictIpResolver.getStrictClientIp(request));
            if (!turnstileResponse.isSuccess() && turnstileResponse.getAction() != "registerEvent"){
                return respond.respond(MediaType.APPLICATION_JSON,401, "message","请先通过Cloudflare Turnstile安全验证喵!","timestamp", LocalDateTime.now());
            }
            String seed = utilset.RandomString(8);
            String uid = utilset.GenerateUUID(UUIDtype.NyanID,true, seed+Base64.getEncoder().encodeToString(email.getBytes(StandardCharsets.UTF_8)));
            String VerificationCode = utilset.RandomString(128);
            String LockPassword = utilset.HMACSHA256(encryptionKey,password);
            emailService.sendmail(email,email, EmailBody.RegisterBody.getBody().replace("${link}",HOST + "/#/verification/" + VerificationCode));
            JSONObject json = new JSONObject();
            json.put("uid", uid);
            json.put("email", email);
            json.put("username", username);
            json.put("password", LockPassword);
            redisService.setValueWithExpiration(VerificationCode, json, 300, TimeUnit.SECONDS);
            redisService.setValueWithExpiration(String.valueOf(Event), "1", 800, TimeUnit.SECONDS);
            return respond.respond(MediaType.APPLICATION_JSON,200, "message","请前往邮箱验证然后完成注册,注意,链接有效期只有5分钟,请尽快验证喵!","timestamp", LocalDateTime.now());

        }else {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","用户名或邮箱已被注册喵!","timestamp", LocalDateTime.now());
        }
    }

    @Transactional
    public ResponseEntity<?> registerConfirm(String code, HttpServletRequest request){
        if (code == null && code.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","The verification code is incorrect or invalid 杂鱼喵!","timestamp", LocalDateTime.now());
        }
        if (redisService.getValue(code) == null){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","The verification code is incorrect or invalid 杂鱼喵!","timestamp", LocalDateTime.now());
        }
        JSONObject data = JSONObject.parseObject(redisService.getValue(code).toString());
        String uid = data.getString("uid");
        String password =data.getString("password");
        String username =data.getString("username");
        String email =data.getString("email");
        Accounts accounts = new Accounts();
        accounts.setUid(uid);
        accounts.setEmail(email);
        accounts.setPassword(password);
        accounts.setUsername(username);
        accounts.setBind(null);
        accounts.setSecretKey(null);
        accounts.setIsActive(true);
        accounts.setRegisterTime(LocalDateTime.now());
        userService.save(accounts);
        NyanIDuser nyanIDuser = new NyanIDuser();
        nyanIDuser.setUid(uid);
        nyanIDuser.setDescription("啊哈,这只猫猫很懒,没有简介啦!");
        nyanIDuser.setNickname("还没想好取啥名字的新猫猫");
        nyanIDuser.setExp(0);
        nyanIDuser.setIsDeveloper(false);
        nyanIDuser.setIsGIFAvatar(false);
        nyanIDuser.setGIFAvatarID(0);
        nyanIDuser.setEnableGIFAvatar(false);
        nyanidUserService.save(nyanIDuser);
        redisService.deleteValue(code);
        return respond.respond(MediaType.APPLICATION_JSON,200, "message","The verification is successful, please go to Login 杂鱼喵~","timestamp", LocalDateTime.now());
    }

    @Transactional
    public ResponseEntity<?> login(String email, String password,String idempotencyKey, HttpServletRequest request) throws Exception {
        if (email == null || email.isEmpty() || password == null || password.isEmpty() || idempotencyKey == null || idempotencyKey.isEmpty()) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","非法参数喵!","timestamp", LocalDateTime.now());
        }
        if(!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}") ){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","邮箱格式错误喵!","timestamp", LocalDateTime.now());
        }
        TurnstileResponse turnstileResponse = TurnstileService.validateToken(idempotencyKey,TurnstileSecretKey,strictIpResolver.getStrictClientIp(request));
        if (!turnstileResponse.isSuccess() && turnstileResponse.getAction() != "LoginEvent"){
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","请先通过Cloudflare Turnstile安全验证喵!","timestamp", LocalDateTime.now());
        }
            JSONObject BanEvent = new JSONObject();
            BanEvent.put(EventID2, email);
            if (redisService.getValue(String.valueOf(BanEvent)) != null && redisService.getValue(String.valueOf(BanEvent)).equals(strictIpResolver.getStrictClientIp(request))) {
                return respond.respond(MediaType.APPLICATION_JSON,401, "message","The account doesn't exist or is locked because of a password error 杂鱼喵!","timestamp", LocalDateTime.now());
            } else {
                if (constMap.get(email) == null) {
                    constMap.put(email, new UserServices.Const(1));
                }else if (constMap.get(email).requestCount > 3) {
                    constMap.remove(email);
                    redisService.setValueWithExpiration(String.valueOf(BanEvent), strictIpResolver.getStrictClientIp(request), 180, TimeUnit.SECONDS);
                }
            }
            Accounts accounts = accountsRepository.GetUser(email);
            if (accounts == null){
                return respond.respond(MediaType.APPLICATION_JSON,401, "message","The account doesn't exist or is locked because of a password error 杂鱼喵!","timestamp", LocalDateTime.now());
            }
            String pwd = utilset.HMACSHA256(encryptionKey,password);
            if (!pwd.equals(accounts.getPassword())){
                if (constMap.get(email) != null) {
                    constMap.get(email).requestCount++;
                }
                return respond.respond(MediaType.APPLICATION_JSON,401, "message","The account doesn't exist or is locked because of a password error 杂鱼喵!","timestamp", LocalDateTime.now());
            }
            BanUserList banUserList = banUserRepository.LEVE450TRUE(accounts.getUid());
            if (banUserList != null){
                constMap.remove(email);
                return respond.respond(MediaType.APPLICATION_JSON,401, "message","异常等级LEVEL"+banUserList.getType()+",异常原因"+banUserList.getReason()+",处罚ID"+banUserList.getBanID()+"账户状态异常杂鱼喵!","timestamp", LocalDateTime.now());
            }
            if (accounts.getSecretKey() != null){
                constMap.remove(email);
                String eid = utilset.RandomString(32);
                JSONObject object = new JSONObject();
                object.put("uid",accounts.getUid());
                object.put("skey",accounts.getSecretKey());
                redisService.setValueWithExpiration(eid,JSONObject.toJSONString(object),10,TimeUnit.MINUTES);
                return respond.respond(MediaType.APPLICATION_JSON,200, "have2fa",true,"Token", utilset.encrypt(eid,publicKey));
            }
                String session = utilset.GetSessionUUID(request, accounts.getUid());
                String uid = userDevicesRepository.findUidBySession(session);
                if (uid == null) {
                    return getResponse(email, request, accounts, session);
                }
                    if (Objects.equals(uid, accounts.getUid()) && utilset.isDaysBefore(userDevicesRepository.findTimeBySession(session), 14)) {
                        String token = userDevicesRepository.findTokenBySession(session);
                        userDevicesRepository.UpdateCreateTime(LocalDateTime.now(), token);
                        return respond.respond(MediaType.APPLICATION_JSON, 200, "have2fa", false, "access_token", utilset.encrypt(token, publicKey), "timestamp", LocalDateTime.now());
                    }else {
                        userDevicesRepository.deleteBySession(session);
                        return getResponse(email, request, accounts, session);
                    }
    }

    public ResponseEntity<?> getResponse(String email, HttpServletRequest request, Accounts accounts, String session) {
        String clientid = utilset.RandomString(32);
        String token = utilset.RandomString(64);
        UserDevices userDevices = new UserDevices();
        userDevices.setUid(accounts.getUid());
        userDevices.setDeviceID("null");
        userDevices.setDeviceName("WEB");
        userDevices.setToken(token);
        userDevices.setIp(strictIpResolver.getStrictClientIp(request));
        userDevices.setIsActive(true);
        userDevices.setSession(session);
        userDevices.setClientId(clientid);
        userDevices.setHardwareID(null);
        userDevices.setCreateTime(LocalDateTime.now());
        userDevicesService.save(userDevices);
        if (constMap.get(email) != null) {
            constMap.remove(email);
        }
        return respond.respond(MediaType.APPLICATION_JSON, 200, "have2fa", false, "access_token", utilset.encrypt(token, publicKey), "timestamp", LocalDateTime.now());
    }

    @Transactional
    public ResponseEntity<?> l2fa(HttpServletRequest request,String verifyCode,String token ) {
        if (verifyCode.isEmpty() && token.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,401,"message","This code or token is not exits","timestamp", LocalDateTime.now());
        }
        if (!verifyCode.matches("[0-9]{3,7}")){
            return respond.respond(MediaType.APPLICATION_JSON,401,"message","This code type is not support","timestamp", LocalDateTime.now());
        }
        String DecryptToken = utilset.decrypt(token, privateKey);
        if (redisService.getValue(DecryptToken) != null) {
            JSONObject value = JSONObject.parseObject(redisService.getValue(DecryptToken).toString());
            String uid = value.getString("uid");
            String skey = value.getString("skey");
            String IP = strictIpResolver.getStrictClientIp(request);
            if (utilset.checkCode(skey, Integer.parseInt(verifyCode))) {
                Accounts accounts = accountsRepository.GetUser(uid);
                String tokend = utilset.RandomString(64);
                UserDevices userDevices = new UserDevices();
                userDevices.setUid(uid);
                userDevices.setDeviceID(utilset.RandomString(16));
                userDevices.setDeviceName("Web");
                userDevices.setToken(tokend);
                userDevices.setIp(IP);
                userDevices.setIsActive(true);
                userDevices.setSession(request.getSession().getId());
                userDevices.setClientId(DecryptToken);
                userDevices.setCreateTime(LocalDateTime.now());
                userDevicesService.save(userDevices);
                if (constMap.get(accounts.getEmail()) != null) {
                    constMap.remove(accounts.getEmail());
                }
                redisService.deleteValue(DecryptToken);
                return respond.respond(MediaType.APPLICATION_JSON,200, "access_token",utilset.encrypt(tokend, publicKey), "timestamp",LocalDateTime.now());
            }else return respond.respond(MediaType.APPLICATION_JSON,401,"message","2FA验证码已失效或登录时间过期","timestamp", LocalDateTime.now());
        }else return respond.respond(MediaType.APPLICATION_JSON,401,"message","登录时间过期","timestamp", LocalDateTime.now());

    }

    private static class Const {
        int requestCount;
        Const(int requestCount) {
            this.requestCount = requestCount;
        }
    }
}
