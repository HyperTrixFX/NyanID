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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
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



/**
 * Constructor for UserServices class that initializes various services and repositories required for user management.
 */
    public UserServices(AccountsRepository accountsRepository, EmailService emailService, RedisService redisService, UserService userService, NyanidUserService nyanidUserService, BanUserRepository banUserRepository, UserDevicesRepository userDevicesRepository, UserDevicesService userDevicesService, StrictIpResolver strictIpResolver, utilset utilset, Respond respond) {
        // Assigning the accounts repository instance
        this.accountsRepository = accountsRepository;
        // Assigning the email service instance
        this.emailService = emailService;
        // Assigning the Redis service instance
        this.redisService = redisService;
        // Assigning the user service instance
        this.userService = userService;
        // Assigning the NyanID user service instance
        this.nyanidUserService = nyanidUserService;
        // Assigning the ban user repository instance
        this.banUserRepository = banUserRepository;
        // Assigning the user devices repository instance
        this.userDevicesRepository = userDevicesRepository;
        // Assigning the user devices service instance
        this.userDevicesService = userDevicesService;
        // Assigning the strict IP resolver instance
        this.strictIpResolver = strictIpResolver;
        // Assigning the utility set instance
        this.utilset = utilset;
        // Assigning the response service instance
        this.respond = respond;
    }


/**
 * Registers a new user account with the provided credentials and performs necessary validations.
 * This method is transactional and handles various edge cases including input validation,
 * email format verification, username restrictions, password requirements, and security checks.
 *
 * @param username The desired username for the new account
 * @param password The password for the new account
 * @param email The email address for the new account
 * @param idempotencyKey A unique key to prevent duplicate registrations
 * @param request The HTTP request object for IP resolution
 * @param response The HTTP response object
 * @return ResponseEntity containing appropriate status and message based on registration outcome
 */
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
            if (!turnstileResponse.isSuccess() && !Objects.equals(turnstileResponse.getAction(), "registerEvent")){
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


/**
 * Handles the registration confirmation process by verifying the provided code and creating user accounts.
 * This method is transactional to ensure data consistency.
 *
 * @param code The verification code sent to the user
 * @param request The HTTP servlet request
 * @return ResponseEntity containing a JSON response with appropriate status and message
 */
    @Transactional
    public ResponseEntity<?> registerConfirm(String code, HttpServletRequest request){
    // Check if the verification code is null or empty
        if (code == null || code.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","The verification code is incorrect or invalid 杂鱼喵!","timestamp", LocalDateTime.now());
        }
    // Verify if the code exists in Redis
        Object redisValue = redisService.getValue(code);
        if (redisValue == null){
            return respond.respond(MediaType.APPLICATION_JSON,403, "message","The verification code is incorrect or invalid 杂鱼喵!","timestamp", LocalDateTime.now());
        }
    // Extract user data from Redis using the verification code
        JSONObject data = JSONObject.parseObject(redisValue.toString());
        String uid = data.getString("uid");
        String password = data.getString("password");
        String username = data.getString("username");
        String email = data.getString("email");

        // Validate that all required fields are present
        if (uid == null || password == null || username == null || email == null) {
            return respond.respond(MediaType.APPLICATION_JSON,400, "message","Invalid registration data 杂鱼喵!","timestamp", LocalDateTime.now());
        }

    // Create and configure the main user account
        Accounts accounts = new Accounts();
        accounts.setUid(uid);
        accounts.setEmail(email);
        accounts.setPassword(password);
        accounts.setUsername(username);
        accounts.setBind(null);
        accounts.setGithubAccessToken(null);
        accounts.setMicrosoftAccount(null);
        accounts.setSecretKey(null);
        accounts.setIsActive(true);
        accounts.setRegisterTime(LocalDateTime.now());
    // Save the main account to the database
        userService.save(accounts);
    // Create and configure the user's NyanID profile
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



/**
 * Handles user login process with various security checks and validations.
 *
 * @param email The user's email address
 * @param password The user's password
 * @param idempotencyKey The idempotency key for request deduplication
 * @param request The HTTP servlet request object
 * @return ResponseEntity containing appropriate response based on login result
 * @throws Exception If any error occurs during the login process
 */
    @Transactional
    public ResponseEntity<?> login(String email, String password, String idempotencyKey, HttpServletRequest request) throws Exception {
    // Validate input parameters
        if (email == null || email.isEmpty() || password == null || password.isEmpty() || idempotencyKey == null || idempotencyKey.isEmpty()) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, "message", "非法参数喵!", "timestamp", LocalDateTime.now());
        }

    // Validate email format
        if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, "message", "邮箱格式错误喵!", "timestamp", LocalDateTime.now());
        }

    // Implement rate limiting using Redis lock
        String lockKey = "login_lock:" + email;
        String lockValue = UUID.randomUUID().toString();
        boolean locked = false;

        try {
        // Try to acquire lock for rate limiting
            locked = redisService.tryLock(lockKey, lockValue, 3, 10);
            if (!locked) {
                return respond.respond(MediaType.APPLICATION_JSON, 429, "message", "登录请求过于频繁，请稍后再试喵!", "timestamp", LocalDateTime.now());
            }
        // Validate Cloudflare Turnstile captcha
            TurnstileResponse turnstileResponse = TurnstileService.validateToken(idempotencyKey, TurnstileSecretKey, strictIpResolver.getStrictClientIp(request));
            if (!turnstileResponse.isSuccess() && !"LoginEvent".equals(turnstileResponse.getAction())) {
                return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "请先通过Cloudflare Turnstile安全验证喵!", "timestamp", LocalDateTime.now());
            }
        // Check if account is banned
            JSONObject banEventKey = new JSONObject();
            banEventKey.put(EventID2, email);
            if (redisService.getValue(String.valueOf(banEventKey)) != null &&
                redisService.getValue(String.valueOf(banEventKey)).equals(strictIpResolver.getStrictClientIp(request))) {
                return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "账户已被锁定，请稍后再试喵!", "timestamp", LocalDateTime.now());
            }

        // Check login attempts count
            String attemptCountKey = "login_attempts:" + email;
            int attemptCount = 0;
            if (redisService.getValue(attemptCountKey) != null){
                String attemptCountStr = redisService.getValue(attemptCountKey).toString();
                attemptCount = attemptCountStr != null ? Integer.parseInt(attemptCountStr) : 0;
            }

        // Handle too many failed attempts
            if (attemptCount >= 3) {
                redisService.setValueWithExpiration(String.valueOf(banEventKey), strictIpResolver.getStrictClientIp(request), 180, TimeUnit.SECONDS);
                return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "登录尝试次数过多，账户已被临时锁定喵!", "timestamp", LocalDateTime.now());
            }
        // Verify user credentials
            Accounts accounts = accountsRepository.GetUser(email);
            if (accounts == null) {
            // Increment failed attempt count
                redisService.setValueWithExpiration(attemptCountKey, String.valueOf(attemptCount + 1), 300, TimeUnit.SECONDS);
                return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "账户不存在或密码错误喵!", "timestamp", LocalDateTime.now());
            }
        // Verify password
            String pwd = utilset.HMACSHA256(encryptionKey, password);
            if (!pwd.equals(accounts.getPassword())) {
            // Increment failed attempt count
                redisService.setValueWithExpiration(attemptCountKey, String.valueOf(attemptCount + 1), 300, TimeUnit.SECONDS);
                return respond.respond(MediaType.APPLICATION_JSON, 401, "message", "账户不存在或密码错误喵!", "timestamp", LocalDateTime.now());
            }
        // Reset failed attempt count on successful login
            redisService.deleteValue(attemptCountKey);

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

        // Handle session management
            String session = utilset.GetSessionUUID(request, accounts.getUid());
            String uid = userDevicesRepository.findUidBySession(session);

            if (uid == null) {
                return getResponse(email, request, accounts, session);
            }

        // Check if session is valid and recent
            if (Objects.equals(uid, accounts.getUid()) && utilset.isDaysBefore(userDevicesRepository.findTimeBySession(session), 14)) {
                String token = userDevicesRepository.findTokenBySession(session);
                userDevicesRepository.UpdateCreateTime(LocalDateTime.now(), token);
                return respond.respond(MediaType.APPLICATION_JSON, 200, "have2fa", false, "access_token", utilset.encrypt(token, publicKey), "timestamp", LocalDateTime.now());
            } else {
            // Create new session if existing one is invalid or expired
                userDevicesRepository.deleteBySession(session);
                return getResponse(email, request, accounts, session);
            }
        } finally {
        // Release the lock if it was acquired
            if (locked) {
                redisService.unlock(lockKey, lockValue);
            }
        }
    }



/**
 * This method generates a response for authentication, creating a new device token and session for the user.
 * It handles the process of setting up a new user device record and returns a formatted response with encrypted token.
 *
 * @param email The user's email address
 * @param request The HttpServletRequest object to extract client IP information
 * @param accounts The user account object containing user details
 * @param session The current session identifier
 * @return ResponseEntity containing the authentication response with encrypted token and metadata
 */
    public ResponseEntity<?> getResponse(String email, HttpServletRequest request, Accounts accounts, String session) {
    // Generate a random client ID and token for the new session
        String clientid = utilset.RandomString(32);
        String token = utilset.RandomString(64);
    // Create a new UserDevices object to store device information
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
    // Save the new device information to the database
        userDevicesService.save(userDevices);
        return respond.respond(MediaType.APPLICATION_JSON, 200, "have2fa", false, "access_token", utilset.encrypt(token, publicKey), "timestamp", LocalDateTime.now());
    }

    
    
    
/**
 * This method handles the two-factor authentication (2FA) process for user login.
 * It validates the verification code and token, then creates a new session if authentication succeeds.
 *
 * @param request The HttpServletRequest object containing the request details
 * @param verifyCode The 2FA verification code provided by the user
 * @param token The encrypted token containing user session information
 * @return ResponseEntity with appropriate status code and message indicating the result of authentication
 */
    @Transactional
    public ResponseEntity<?> l2fa(HttpServletRequest request, String verifyCode, String token) {
    // Check if either verification code or token is empty
        if (verifyCode == null || verifyCode.isEmpty() || token == null || token.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,401,"message","This code or token is not exists","timestamp", LocalDateTime.now());
        }
    // Validate the format of the verification code (should be 3-7 digits)
        if (!verifyCode.matches("[0-9]{3,7}")){
            return respond.respond(MediaType.APPLICATION_JSON,401,"message","This code type is not support","timestamp", LocalDateTime.now());
        }
    // Decrypt the token using private key
        String DecryptToken;
        try {
            DecryptToken = utilset.decrypt(token, privateKey);
        } catch (Exception e) {
            return respond.respond(MediaType.APPLICATION_JSON,401,"message","Invalid token","timestamp", LocalDateTime.now());
        }
    // Check if the decrypted token exists in Redis
        if (redisService.getValue(DecryptToken) != null) {
        // Parse the token value from Redis to get user information
            JSONObject value = JSONObject.parseObject(redisService.getValue(DecryptToken).toString());
            String uid = value.getString("uid");
            String skey = value.getString("skey");
        // Get the client IP address from the request
            String IP = strictIpResolver.getStrictClientIp(request);
        // Verify the provided code against the stored secret key
            int code;
            try {
                code = Integer.parseInt(verifyCode);
            } catch (NumberFormatException e) {
                return respond.respond(MediaType.APPLICATION_JSON,401,"message","Invalid verification code format","timestamp", LocalDateTime.now());
            }
            
            if (utilset.checkCode(skey, code)) {
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
                redisService.deleteValue(DecryptToken);
            // Return success response with encrypted access token
                return respond.respond(MediaType.APPLICATION_JSON,200, "access_token",utilset.encrypt(tokend, publicKey), "timestamp",LocalDateTime.now());
            }else {
                return respond.respond(MediaType.APPLICATION_JSON,401,"message","2FA验证码已失效或登录时间过期","timestamp", LocalDateTime.now());
            }
        } else {
            return respond.respond(MediaType.APPLICATION_JSON,401,"message","登录时间过期","timestamp", LocalDateTime.now());
        }
    }



}
