package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.NyanIdApplication;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.utils.EmailHelper.EmailService;
import moe.koseirin.nyanruaineo.utils.EnumList.EmailBody;
import moe.koseirin.nyanruaineo.utils.EnumList.UUIDtype;
import moe.koseirin.nyanruaineo.utils.ErrUtils.SJson;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.NyanidUserService;
import moe.koseirin.nyanruaineo.utils.SqlUtils.Service.UserService;
import moe.koseirin.nyanruaineo.utils.WebMvc.IPSecurityDetection;
import moe.koseirin.nyanruaineo.utils.WebMvc.TurnstileResponse;
import moe.koseirin.nyanruaineo.utils.WebMvc.TurnstileService;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import static moe.koseirin.nyanruaineo.utils.Respond.respond;

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

    @Value("${NyanidSetting.encryptionKey}")
    private String encryptionKey;

    @Value("${NyanidSetting.TurnstileSecretKey}")
    private String TurnstileSecretKey;

    @Value("${NyanidSetting.HOST}")
    private String HOST;

    @Value("${NyanidSetting.EnableUserRegister}")
    private boolean EnableUserRegister;
    public String EventID = "RegEvent1";

    public UserServices(AccountsRepository accountsRepository, EmailService emailService, RedisService redisService, UserService userService, NyanidUserService nyanidUserService) {
        this.accountsRepository = accountsRepository;
        this.emailService = emailService;
        this.redisService = redisService;
        this.userService = userService;
        this.nyanidUserService = nyanidUserService;
    }


    @Transactional
    public ResponseEntity<?> register(String username, String password, String email,String idempotencyKey, HttpServletRequest request, HttpServletResponse response) {
        if (!EnableUserRegister) {
            return respond(MediaType.APPLICATION_JSON,403, "message","服务器未开启用户注册功能喵!","timestamp", LocalDateTime.now());
        }

        if (username == null || username.isEmpty() || password == null || password.isEmpty() || email == null || email.isEmpty() || idempotencyKey == null || idempotencyKey.isEmpty()) {
            return respond(MediaType.APPLICATION_JSON,403, "message","非法参数喵!","timestamp", LocalDateTime.now());
        }

        if(!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}") ){
            return respond(MediaType.APPLICATION_JSON,403, "message","邮箱格式错误喵!","timestamp", LocalDateTime.now());
        }

        if (!username.matches("(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,20}")){
            return respond(MediaType.APPLICATION_JSON,403, "message","用户名只允许英文,数字和下划线组合喵!","timestamp", LocalDateTime.now());
        }

        if (NyanIdApplication.wordBs.contains(username)) {
            redisService.setValueWithExpiration(IPSecurityDetection.getIpAddress(request),1,1, TimeUnit.MINUTES);
            return respond(MediaType.APPLICATION_JSON,403, "message","非法用户名,请先仔细阅读我们的服务条款再进行注册喵!","timestamp", LocalDateTime.now());
        }

        if (accountsRepository.findByEmail(email) == null && accountsRepository.GetUser(username) == null) {
            JSONObject Event = new JSONObject();
            Event.put(EventID,email);
            if (redisService.getValue(String.valueOf(Event)) != null) {
                return respond(MediaType.APPLICATION_JSON,403, "message","该邮箱已被暂时冻结注册喵!","timestamp", LocalDateTime.now());
            }
            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,20}$")){
                return respond(MediaType.APPLICATION_JSON,403, "message","您的密码至少包含一个大写字母(A-Z),包含一个小写字母(a-z),包含一个数字(0-9),包含一个特殊字符,且长度至少为 9 个字符喵!","timestamp", LocalDateTime.now());
            }
            TurnstileResponse turnstileResponse = TurnstileService.validateToken(idempotencyKey,TurnstileSecretKey,IPSecurityDetection.getIpAddress(request));
            if (!turnstileResponse.isSuccess() && turnstileResponse.getAction() != "registerEvent"){
                return respond(MediaType.APPLICATION_JSON,401, "message","请先通过Cloudflare Turnstile安全验证喵!","timestamp", LocalDateTime.now());
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
            return respond(MediaType.APPLICATION_JSON,200, "message","请前往邮箱验证然后完成注册,注意,链接有效期只有5分钟,请尽快验证喵!","timestamp", LocalDateTime.now());

        }else {
            return respond(MediaType.APPLICATION_JSON,403, "message","用户名或邮箱已被注册喵!","timestamp", LocalDateTime.now());
        }
    }



    @Transactional
    public ResponseEntity<?> registerConfirm(String code, HttpServletRequest request){
        if (code == null && code.isEmpty()){
            return respond(MediaType.APPLICATION_JSON,403, "message","The verification code is incorrect or invalid 杂鱼喵!","timestamp", LocalDateTime.now());
        }
        if (redisService.getValue(code) == null){
            return respond(MediaType.APPLICATION_JSON,403, "message","The verification code is incorrect or invalid 杂鱼喵!","timestamp", LocalDateTime.now());
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
        return respond(MediaType.APPLICATION_JSON,200, "message","The verification is successful, please go to Login 杂鱼喵~","timestamp", LocalDateTime.now());
    }



    @Transactional
    public ResponseEntity<?> login(String username, String password, String email, HttpServletRequest request) {


        return null;
    }

    @Transactional
    public ResponseEntity<?> l2fa(String username, String password, String email, HttpServletRequest request) {


        return null;
    }

}
