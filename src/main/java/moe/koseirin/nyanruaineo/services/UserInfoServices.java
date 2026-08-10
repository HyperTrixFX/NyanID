package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.dto.UserResponse;
import moe.koseirin.nyanruaineo.dto.UserResponseDTO;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.entity.UserPermissions;
import moe.koseirin.nyanruaineo.repository.*;
import moe.koseirin.nyanruaineo.dto.BanUserListJson;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.SqlService.impl.BanUserServiceImpl;
import moe.koseirin.nyanruaineo.utils.SqlService.impl.UserServiceImpl;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/*
 * @author KoseiRin_
 * awa
 */
@Service
public class UserInfoServices {

    private final NyanIDuserRepository nyanIDuserRepository;
    private final AccountsRepository accountsRepository;
    private final BanUserRepository banUserRepository;
    private final utilset utilset;
    private final UserServiceImpl userService;
    private final Respond respond;
    private final UserPermissionsRepository userPermissionsRepository;
    private final UserDevicesRepository userDevicesRepository;
    private final YggdrasilRepository yggdrasilRepository;
    private final BanUserServiceImpl banUserService;

    @Value("${yggdrasil.APILocation}")
    private String APILocation;

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    public UserInfoServices(NyanIDuserRepository nyanIDuserRepository, AccountsRepository accountsRepository, BanUserRepository banUserRepository, utilset utilset, UserServiceImpl userService1, Respond respond, UserPermissionsRepository userPermissionsRepository, UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, BanUserServiceImpl banUserService) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.accountsRepository = accountsRepository;
        this.banUserRepository = banUserRepository;
        this.utilset = utilset;
        this.userService = userService1;
        this.respond = respond;
        this.userPermissionsRepository = userPermissionsRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.banUserService = banUserService;
    }

    @Transactional
    public ResponseEntity<?> validateMethod(HttpServletRequest request){

            return null;
    }

    @Transactional
    public ResponseEntity<?> getPublicUserInfo(String uuid, HttpServletRequest request){
        if (banUserRepository.LEVE450TRUE(uuid) == null ) {
            if (accountsRepository.GetUser(uuid) != null){
                Accounts accounts = accountsRepository.GetUser(uuid);
                NyanIDuser user = nyanIDuserRepository.getUser(accounts.getUid());
                int exp = user.getExp();
                Boolean isDeveloper = user.isIsDeveloper();
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("nickname",user.getNickname());
                jsonObject.put("exp",exp);
                jsonObject.put("description",user.getDescription());
                jsonObject.put("username",accounts.getUsername());
                jsonObject.put("isDeveloper",isDeveloper);
                jsonObject.put("uid",accounts.getUid());
                if (banUserRepository.COUNTByUid(user.getUid()) !=  0){
                    jsonObject.put("ViolationHistory",true);
                    jsonObject.put("NACCOUNT",banUserRepository.COUNTByUid(user.getUid()));
                }
                return respond.respond(MediaType.APPLICATION_JSON,200,jsonObject);
            }else {
                return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
            }
        }else {
            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("nickname","该用户已被禁封");
            jsonObject1.put("exp",-1000);
            jsonObject1.put("description","该用户因为违反我们的服务条款或账号准则已被NAC冻结. ");
            jsonObject1.put("username","Neko-AntiCheat BANNED");
            jsonObject1.put("isDeveloper",false);
            jsonObject1.put("uid",uuid);
            return respond.respond(MediaType.APPLICATION_JSON,200,jsonObject1);
        }
    }


    @Transactional
    public ResponseEntity<?> SearchUserApi(UserResponseDTO userResponseDTO){
        if (userResponseDTO == null){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        String value = userResponseDTO.getValue();
        if (value == null) {
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        if (value.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        List<UserResponse> result = userService.searchUsers(value);
        if (result.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Account","timestamp", LocalDateTime.now());
        }
        return ResponseEntity.ok().body(result);

    }

    public ResponseEntity<?> getUserInfo(HttpServletRequest request) {
        String Authorization = request.getHeader("Authorization");
        String raw = Authorization.replace("Bearer ", "").replace(" ", "");
        if (Objects.equals(raw, "undefined")) {
            return ResponseEntity.status(401).build();
        }else {
            String Token = utilset.decrypt(raw,privateKey);
            String uid = userDevicesRepository.findUidByToken(Token);
            Accounts accounts = accountsRepository.GetUser(uid);
            NyanIDuser user = nyanIDuserRepository.getUser(uid);
            String yggdrasilplayeruuid = yggdrasilRepository.GetPlayerUUID(uid);
            int exp = user.getExp();
            Boolean isDeveloper = user.isIsDeveloper();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("nickname",user.getNickname());
            jsonObject.put("exp",exp);
            jsonObject.put("description",user.getDescription());
            jsonObject.put("username",accounts.getUsername());
            jsonObject.put("isDeveloper",isDeveloper);
            jsonObject.put("email",accounts.getEmail());
            jsonObject.put("url",APILocation);
            jsonObject.put("uid",uid);
            if (user.getIsGIFAvatar()){
                jsonObject.put("IsGIFAvatar",true);
                jsonObject.put("EnableGIFAvatar",user.getEnableGIFAvatar());
                jsonObject.put("AvatarID",user.getGIFAvatarID());
            }else {
                jsonObject.put("IsGIFAvatar",false);
            }
            if (accounts.getSecretKey() != null){
                jsonObject.put("have2fa",true);
            }else {
                jsonObject.put("have2fa",false);
            }
            if (userPermissionsRepository.getByUid(uid) != null){
                UserPermissions userPermissions = userPermissionsRepository.getByUid(uid);
                jsonObject.put("isAdmin",true);
                jsonObject.put("akey",userPermissions.getAccessKey());
                jsonObject.put("PermissionsLevel",userPermissions.getLevel());
                jsonObject.put("UserGroup",userPermissions.getUserGroup());
            }
            if (yggdrasilplayeruuid != null){
                jsonObject.put("HasYggdrasilAccount",true);
                jsonObject.put("YggdrasilUUID",yggdrasilplayeruuid);
            }else {
                jsonObject.put("HasYggdrasilAccount",false);
            }
            if (accounts.getBind() != null){
                String MCUUID = accounts.getBind();
                jsonObject.put("bma",true);
                jsonObject.put("mcuid",MCUUID);
            }
            return respond.respond(MediaType.APPLICATION_JSON,200,jsonObject);
        }

    }

    public ResponseEntity<?> ViolationHistory(HttpServletRequest request) {
        String Authorization = request.getHeader("Authorization");
        String raw = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(raw,privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
        List<BanUserListJson> result = banUserService.GetBanList(uid);
        if (result.isEmpty()){
            return respond.respond(MediaType.APPLICATION_JSON,404, "message","Not Found Violation History","timestamp", LocalDateTime.now());
        }else {
            return respond.respond(MediaType.APPLICATION_JSON,200,result);
        }
    }
}
