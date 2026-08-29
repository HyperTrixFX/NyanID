package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.BanUserList;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.repository.AccountsRepository;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import moe.koseirin.nyanruaineo.repository.NyanIDuserRepository;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.services.impl.UserDataImpl;
import moe.koseirin.nyanruaineo.utils.EmailHelper.EmailService;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.WebMvc.StrictIpResolver;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UserDataServices {

    private final AccountsRepository accountsRepository;
    private final EmailService emailService;
    private final RedisService redisService;
    private final utilset utilset;
    private final Respond respond;
    private final BanUserRepository banUserRepository;
    private final UserDevicesRepository userDevicesRepository;
    private final StrictIpResolver strictIpResolver;
    private final UserDataImpl userDataImpl;
    private final NyanIDuserRepository nyanIDuserRepository;

    public UserDataServices(AccountsRepository accountsRepository, EmailService emailService, RedisService redisService, utilset utilset, Respond respond, BanUserRepository banUserRepository, UserDevicesRepository userDevicesRepository, StrictIpResolver strictIpResolver, UserDataImpl userDataImpl, NyanIDuserRepository nyanIDuserRepository) {
        this.accountsRepository = accountsRepository;
        this.emailService = emailService;
        this.redisService = redisService;
        this.utilset = utilset;
        this.respond = respond;
        this.banUserRepository = banUserRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.strictIpResolver = strictIpResolver;
        this.userDataImpl = userDataImpl;
        this.nyanIDuserRepository = nyanIDuserRepository;
    }

    public String EventID = "FP1";

    @Value("${NyanidSetting.encryptionKey}")
    private String encryptionKey;

    @Value("${yggdrasil.publicKey}")
    private String  publicKey;

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;



    @Transactional
    public ResponseEntity<?> ForgetPwdStep1(String email, HttpServletRequest request){
        if (email == null) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","非法参数喵!","timestamp", LocalDateTime.now());
        }
        if(!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}") ){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","邮箱格式错误喵!","timestamp", LocalDateTime.now());
        }
        JSONObject Event = new JSONObject();
        Event.put(EventID,email);
        if (redisService.getValue(String.valueOf(Event)) != null) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","请稍后重试喵!","timestamp", LocalDateTime.now());
        }
        Accounts accounts = accountsRepository.GetUser(email);
        if (accounts == null) {
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","The account doesn't exist or is locked because of a password error 杂鱼喵!","timestamp", LocalDateTime.now());
        }
        BanUserList banUserList = banUserRepository.LEVE450TRUE(accounts.getUid());
        if (banUserList != null){
            return respond.respond(MediaType.APPLICATION_JSON,401, "message","异常等级LEVEL"+banUserList.getType()+",异常原因"+banUserList.getReason()+",处罚ID"+banUserList.getBanID()+"账户状态异常,无法修改密码杂鱼喵!","timestamp", LocalDateTime.now());
        }
        String token = utilset.GetSessionUUID(request,accounts.getUid());
        String code = utilset.RandomString(8);
        JSONObject object = new JSONObject();
        object.put("uid",accounts.getUid());
        object.put("token",token);
        object.put("email",email);
        redisService.setValueWithExpiration(code,object,300, java.util.concurrent.TimeUnit.SECONDS);
        redisService.setValueWithExpiration(String.valueOf(Event),1,300, java.util.concurrent.TimeUnit.SECONDS);
        emailService.sendVerificationCode(email,code);
        return respond.respond(MediaType.APPLICATION_JSON,200,"message","验证码已发送至邮箱","token",utilset.encrypt(token,publicKey),"timestamp",LocalDateTime.now());
    }

    @Transactional
    public ResponseEntity<?> ForgetPwdStep2(String token,String code,String pwd, HttpServletRequest request){
        if (token == null || token.isEmpty() || code == null || code.isEmpty() || pwd == null || pwd.isEmpty()) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","非法参数喵!","timestamp", LocalDateTime.now());
        }
        if (!pwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,20}$")){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","您的密码至少包含一个大写字母(A-Z),包含一个小写字母(a-z),包含一个数字(0-9),包含一个特殊字符,且长度至少为 9 个字符喵!","timestamp", LocalDateTime.now());
        }
        if (redisService.getValue(code) == null) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","参数错误喵!","timestamp", LocalDateTime.now());
        }
        JSONObject object = JSONObject.parseObject(redisService.getValue(code).toString());
        if (utilset.decrypt(token,privateKey) == null){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","参数错误喵!","timestamp", LocalDateTime.now());
        }
        if (!Objects.equals(object.getString("token"),utilset.decrypt(token,privateKey))) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","验证码错误或已过期喵!","timestamp", LocalDateTime.now());
        }
        String uid = object.getString("uid");
        accountsRepository.UpdatePassword(uid, utilset.HMACSHA256(encryptionKey,pwd));
        emailService.NotificationEmail(object.getString("email"), strictIpResolver.getStrictClientIp(request), "修改密码", uid);
        return respond.respond(MediaType.APPLICATION_JSON,200,"message","The password was successfully changed 杂鱼喵~","timestamp",LocalDateTime.now());
    }

    @Transactional
    public ResponseEntity<?> ActionMethod(HttpServletRequest request, int action, String nickname, String username, String description, String code){
        Accounts accounts = GetUser(request);
        if (accounts == null) {
            return respond.respond(MediaType.APPLICATION_JSON,500, "message","未知登录绕过喵！！！","timestamp", LocalDateTime.now());
        }
        NyanIDuser nyanIDuser = nyanIDuserRepository.getUser(accounts.getUid());
        return switch (action) {
            case 0 -> // 设置昵称
                    userDataImpl.handleUpdateNickname(nickname, nyanIDuser, accounts.getUid());
            case 1 -> // 更改用户名
                    userDataImpl.handleUpdateUsername(username, accounts, request);
            case 2 -> // 更改简介
                    userDataImpl.handleUpdateDescription(description, accounts.getUid());
            case 3 -> // 绑定Minecraft账号
                    userDataImpl.handleBindMinecraft(code, accounts);
            case 4 -> // 切换头像模式
                    userDataImpl.handleToggleAvatarMode(accounts.getUid());
            default ->
                    respond.respond(MediaType.APPLICATION_JSON, 400, "message", "RequestBody action is invalid MiaoWu~", "timestamp", LocalDateTime.now());
        };
    }

    @Transactional
    public ResponseEntity<?> PutMethod(MultipartFile avatar, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (avatar == null) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","RequestParam avatar is NULL  MiaoWu~","timestamp", LocalDateTime.now());
        }
        if (request.getContentType() == null || !request.getContentType().matches("multipart/form-data.*")) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","RequestParam avatar is NULL  MiaoWu~","timestamp", LocalDateTime.now());
        }
        if (avatar.getContentType() == null) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","RequestParam avatar is NULL  MiaoWu~","timestamp", LocalDateTime.now());
        }

        String contentType = avatar.getContentType();
        if (!contentType.matches("image/.*")) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","RequestParam avatar is not image MiaoWu~","timestamp", LocalDateTime.now());
        }

        if (avatar.getSize() >= 1024 * 1024 * 10) {
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","RequestParam avatar is not image MiaoWu~","timestamp", LocalDateTime.now());
        }
        Accounts accounts = GetUser(request);
        SaveUserAvatar(accounts.getUid(), avatar);
        return respond.respond(MediaType.APPLICATION_JSON,200, "message","Setting avatar success MiaoWu~","timestamp", LocalDateTime.now());

    }

    private Accounts GetUser(HttpServletRequest request) {
        String Authorization = request.getHeader("Authorization");
        String rawToken = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(rawToken, privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
        return accountsRepository.GetUser(uid);
    }

    private void SaveUserAvatar(String uid, MultipartFile avatar) throws IOException {
        Path UserAvatar = Paths.get("Data/UserAvatar/UA-");
        File originalFile = new File(UserAvatar+uid+".png");
        if (originalFile.isFile()) {
            originalFile.delete();
            utilset.reduceImageByRatio(avatar.getInputStream(), UserAvatar,uid,1, 1);
        }else {
            utilset.reduceImageByRatio(avatar.getInputStream(), UserAvatar,uid,1, 1);
        }
    }



}
