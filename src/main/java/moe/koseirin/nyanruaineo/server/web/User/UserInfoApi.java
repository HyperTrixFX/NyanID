package moe.koseirin.nyanruaineo.server.web.User;
/*
 * @author KoseiRin_
 * awa
 */

//获取用户信息

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.koseirin.nyanruaineo.entity.Accounts;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.repository.*;
import moe.koseirin.nyanruaineo.entity.UserPermissions;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


@RestController
@RequestMapping("api/zako/v1/userinfo")
public class UserInfoApi {


    private final NyanIDuserRepository nyanIDuserRepository;

    private final AccountsRepository accountsRepository;

    private final UserDevicesRepository userDevicesRepository;

    private final YggdrasilRepository yggdrasilRepository;

    private final UserPermissionsRepository userPermissionsRepository;

    private final utilset utilset;

    @Value("${yggdrasil.APILocation}")
    private String APILocation;

    @Value("${yggdrasil.privateKey}")
    private String  privateKey;

    public UserInfoApi(NyanIDuserRepository nyanIDuserRepository, AccountsRepository accountsRepository, UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, UserPermissionsRepository userPermissionsRepository, utilset utilset) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.accountsRepository = accountsRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.userPermissionsRepository = userPermissionsRepository;
        this.utilset = utilset;
    }


    @GetMapping(produces = "application/json")
    public Object GETMethod(HttpServletResponse response, HttpServletRequest request) {
            String Authorization = request.getHeader("Authorization");
            String raw = Authorization.replace("Bearer ", "").replace(" ", "");
            if (Objects.equals(raw, "undefined")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
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
                    jsonObject.put("Aaction",userPermissions.getLevel());
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
                return jsonObject;
            }

    }
}
