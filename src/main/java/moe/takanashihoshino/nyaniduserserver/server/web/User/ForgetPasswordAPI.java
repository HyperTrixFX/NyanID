package moe.takanashihoshino.nyaniduserserver.server.web.User;
//忘记密码


import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.SJson;
import moe.takanashihoshino.nyaniduserserver.utils.RedisUtils.RedisService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.AccountsRepository;
import moe.takanashihoshino.nyaniduserserver.utils.EmailHelper.EmailService;
import moe.takanashihoshino.nyaniduserserver.utils.OtherUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("api/zako/v1/forgetpwd")
public class ForgetPasswordAPI {

private final AccountsRepository accountsRepository;
private final EmailService emailService;
private final RedisService redisService;

    public ForgetPasswordAPI(AccountsRepository accountsRepository, EmailService emailService, RedisService redisService) {
        this.accountsRepository = accountsRepository;
        this.emailService = emailService;
        this.redisService = redisService;
    }

    public String EventID = "FP1";

@Value("${NyanidSetting.encryptionKey}")
private String encryptionKey;

@Async
@PostMapping(produces = "application/json")
public <T> Object VerifyCode(@RequestBody(required = false) T data, HttpServletResponse response, HttpServletRequest request) throws NoSuchAlgorithmException, InvalidKeyException {
        if (data != null){
            JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(data));
            String email = jsonObject.getString("email");
            String code = jsonObject.getString("code");
            String newpwd = jsonObject.getString("password");
            String ip = request.getRemoteAddr();
            if (email != null && code != null){
                if (email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")){
                    if (redisService.getValue(code) != null){
                        if (!newpwd.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$") | newpwd.length() < 7) {
                            return ErrRes.IllegalRequestException("您的密码至少包含一个大写字母(A-Z),包含一个小写字母(a-z),包含一个数字(0-9),包含一个特殊字符,且长度至少为 8 个字符喵~", response);
                        } else {
                            redisService.deleteValue(code);
                            String pwd = OtherUtils.HMACSHA256(encryptionKey, newpwd);
                            String uid = accountsRepository.findByEmail(email);
                            accountsRepository.UpdatePassword(email, pwd);
                            emailService.NotificationEmail(email, ip, "修改密码", uid);
                            SJson sJson = new SJson();
                            sJson.setMessage("The password was successfully changed 杂鱼喵~");
                            sJson.setStatus(200);
                            sJson.setTimestamp(LocalDateTime.now());
                            return sJson;
                        }
                    }else return ErrRes.IllegalClientException("验证码错误或已过期", response);
                }else return ErrRes.IllegalRequestException("邮箱格式错误", response);
            }else return ErrRes.IllegalRequestException("参数错误", response);
        }else return ErrRes.IllegalRequestException("参数错误", response);
    }
@GetMapping
public Object GetAuthenticateEmail(@RequestParam(value = "email",required = false) String email, HttpServletResponse response){
    if (email != null){
        if (email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(EventID, email);
            if (accountsRepository.findByEmail(email) != null) {
                if (redisService.getValue(String.valueOf(jsonObject)) != null){
                    return ErrRes.IllegalClientException("请勿重复发送验证码", response);
                }else {
                    String code = OtherUtils.RandomString(8);
                    redisService.setValueWithExpiration(code, email, 300, java.util.concurrent.TimeUnit.SECONDS);
                    emailService.sendVerificationCode(email, code);
                    SJson sJson = new SJson();
                    sJson.setMessage("验证码已发送至邮箱");
                    sJson.setStatus(200);
                    sJson.setTimestamp(LocalDateTime.now());
                    redisService.setValueWithExpiration(String.valueOf(jsonObject), "1", 360, java.util.concurrent.TimeUnit.SECONDS);
                    return sJson;
                }
            } else return ErrRes.NotFoundAccountException("账号不存在或尝试找回次数过多", response);
        }else return ErrRes.IllegalRequestException("邮箱格式错误", response);
    }else return ErrRes.IllegalRequestException("参数错误", response);
}



}
