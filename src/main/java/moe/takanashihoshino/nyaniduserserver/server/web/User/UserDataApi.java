package moe.takanashihoshino.nyaniduserserver.server.web.User;
//用户数据修改

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.EmailHelper.EmailService;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.Error;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrorCode;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.SJson;
import moe.takanashihoshino.nyaniduserserver.utils.OtherUtils;
import moe.takanashihoshino.nyaniduserserver.utils.RedisUtils.RedisService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Accounts;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.NyanIDuser;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.AccountsRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.NyanIDuserRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.UserDevicesRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.YggdrasilRepository;
import moe.takanashihoshino.nyaniduserserver.websocket.server.BungeeConnectHandle;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;

@RestController
@RequestMapping("api/zako/v1/userdata")
public class UserDataApi {


    private final NyanIDuserRepository nyanIDuserRepository;


    private final UserDevicesRepository userDevicesRepository;


    private final YggdrasilRepository yggdrasilRepository;


    private final AccountsRepository accountsRepository;


    private final EmailService emailService;


    private final RedisService redisService;

    public UserDataApi(NyanIDuserRepository nyanIDuserRepository, UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, AccountsRepository accountsRepository, EmailService emailService, RedisService redisService) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.accountsRepository = accountsRepository;
        this.emailService = emailService;
        this.redisService = redisService;
    }

    @Async
    @PostMapping(produces = "application/json")
    public <T> Object PostMethod(@RequestBody(required = false) T data, HttpServletResponse response, HttpServletRequest request){
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        NyanIDuser user = nyanIDuserRepository.getUser(uid);
        Accounts accounts = accountsRepository.GetUser(uid);
        if (data != null){
           JSONObject a = JSONObject.parseObject(JSONObject.toJSONString(data));
           int Action = a.getIntValue("action");
        switch (Action){
                        case 0 :{//设置昵称
                            String NICKNAME = a.getString("nickname");
                            if (NICKNAME != null && NICKNAME.length() > 2){
                                if (!Objects.equals(user.getNickname(), NICKNAME)){
                                    nyanIDuserRepository.UpdateNickname(NICKNAME,uid);
                                    response.setStatus(200);
                                    return success("Setting nickname success MiaoWu~",200);
                                }else return ErrRes.IllegalRequestException("The nickname is the same as in the server  MiaoWu~",response);
                            }else return ErrRes.IllegalRequestException("nickname is invalid  MiaoWu~",response);
                        }
                        case 1 :{//更改用户名
                            String username = a.getString("username");
                            if (username != null && username.length() > 3 && username.matches("(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,20}")) {
                                Accounts accounts1 = accountsRepository.GetUser(username);
                                if (accounts != null && accounts1 == null){
                                    if (yggdrasilRepository.GetPlayerNAME(uid) != null){
                                        yggdrasilRepository.UpdatePlayerName(username,uid);
                                    }
                                    accountsRepository.UpdateUsername(username,uid);
                                    emailService.NotificationEmail(accountsRepository.GetEmailByUid(uid),request.getRemoteAddr(),"Change username",uid);
                                    userDevicesRepository.LogOut(uid,"Minecraft");
                                    return success("Setting username success MiaoWu~",200);
                                }else return ErrRes.IllegalRequestException("username is already exist  MiaoWu~",response);
                            }else return ErrRes.IllegalRequestException("username is invalid  MiaoWu~",response);
                        }
                        case 2 :{//更改简介
                            String description = a.getString("description");
                            if (description != null && description.length() > 2 && description.length() < 100){
                                nyanIDuserRepository.SetDescriptionByUid(description,uid);
                                return success("Setting description success MiaoWu~",200);
                            }else return ErrRes.IllegalRequestException("description is invalid  MiaoWu~",response);
                        }
                        case 3 :{//bind mc
                                    String code = a.getString("code");
                                    if (!a.getString("code").isEmpty()){
                                        Object UUID = redisService.getValue(code);
                                        if (UUID != null){
                                            accountsRepository.BindMinecraftAccount(UUID.toString(), uid);
                                            redisService.deleteValue(code);
                                            SJson s = new SJson();
                                            s.setStatus(200);
                                            s.setMessage("绑定成功喵~,uuid: "+UUID);
                                            s.setTimestamp(LocalDateTime.now());
                                            JSONObject j = new JSONObject();
                                            j.put("packet","S01");
                                            j.put("uuid",UUID.toString());
                                            j.put("nuid",accounts.getUid());
                                            BungeeConnectHandle.sendMessage(JSONObject.toJSONString(j));
                                            return JSONObject.toJSONString(s);
                                        }else return ErrRes.NotFoundAccountException("无效的绑定码杂鱼喵~",response);
                                    }else return ErrRes.IllegalRequestException("code is invalid  MiaoWu~",response);

                        }
                        case 4 :{//切换头像模式
                            Boolean IsGIFAvatar = nyanIDuserRepository.IsGIFAvatar(uid);
                            if (IsGIFAvatar){
                                Boolean EnableGIFAvatar = nyanIDuserRepository.EnableGIFAvatar(uid);
                                if (EnableGIFAvatar){
                                    nyanIDuserRepository.UpdateEnableGIFAvatar(false,uid);
                                    response.setStatus(204);
                                    return null;
                                }else {
                                    nyanIDuserRepository.UpdateEnableGIFAvatar(true,uid);
                                    response.setStatus(204);
                                    return null;
                                }
                            }else {
                                return ErrRes.IllegalRequestException("NULL",response);
                            }
                        }

            default:
                return ErrRes.IllegalRequestException("RequestBody action is invalid  MiaoWu~",response);
         }
        }else return ErrRes.IllegalRequestException("RequestBody  is NULL  MiaoWu~",response);
    }


    @PutMapping(produces = "application/json")
    public <T> Object PutMethod(@RequestParam(value = "avatar", required = false) T avatar, HttpServletRequest request , HttpServletResponse response) throws IOException {
        if (avatar  != null ){
            MultipartFile avatarFile = (MultipartFile) avatar;
            if (!request.getContentType().matches("multipart/form-data") &&avatarFile.getContentType() != null){
                String ContentType = avatarFile.getContentType();
                if (ContentType.matches("image/.*") && avatarFile.getSize() < 1024 * 1024 * 10) {
                    String Authorization = request.getHeader("Authorization");
                    String Token = Authorization.replace("Bearer ", "").replace(" ", "");
                    String uid = userDevicesRepository.findUidByToken(Token);
                    SaveUserAvatar(uid, avatarFile);
                    SJson sJson = new SJson();
                    sJson.setMessage("Setting avatar success MiaoWu~");
                    sJson.setStatus(200);
                    sJson.setTimestamp(LocalDateTime.now());
                    return JSONObject.toJSONString(sJson);
                }else return ErrRes.IllegalRequestException("RequestParam avatar is not image MiaoWu~",response);
            }else {
                return ErrRes.IllegalRequestException("RequestParam avatar is NULL  MiaoWu~",response);
            }
        }else return ErrRes.IllegalRequestException("RequestParam avatar is NULL  MiaoWu~",response);
    }

    @RequestMapping(produces = "application/json")
    public Object OtherMethod(HttpServletResponse response){
        Error error = new Error();
        error.setError(ErrorCode.MethodNotAllowed.getMessage());
        error.setMessage("Wrong action event MiaoWu~");
        error.setStatus(ErrorCode.MethodNotAllowed.getCode());
        error.setTimestamp(LocalDateTime.now());
        response.setStatus(405);
        return JSONObject.toJSONString(error);
    }

    @Async
    public void SaveUserAvatar(String uid, MultipartFile avatar) throws IOException {
        Path UserAvatar = Paths.get("Data/UserAvatar/UA-");
        File originalFile = new File(UserAvatar+uid+".png");
        if (originalFile.isFile()) {
            originalFile.delete();
            OtherUtils.reduceImageByRatio(avatar.getInputStream(), UserAvatar,uid,1, 1);
        }else {
            OtherUtils.reduceImageByRatio(avatar.getInputStream(), UserAvatar,uid,1, 1);
        }
//        String newavatarMD5 = DigestUtils.md5DigestAsHex(avatar.getBytes());
    }

    public static SJson success(String message,int code){
        SJson sJson = new SJson();
        sJson.setMessage(message);
        sJson.setStatus(code);
        sJson.setTimestamp(LocalDateTime.now());
        return sJson;
    }


    }
