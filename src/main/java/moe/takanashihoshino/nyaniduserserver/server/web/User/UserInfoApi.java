package moe.takanashihoshino.nyaniduserserver.server.web.User;
//获取用户信息

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Accounts;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.NyanIDuser;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.*;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.UserPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("api/zako/v1/userinfo")
public class UserInfoApi {


    private final NyanIDuserRepository nyanIDuserRepository;

    private final AccountsRepository accountsRepository;

    private final UserDevicesRepository userDevicesRepository;

    private final YggdrasilRepository yggdrasilRepository;

    private final UserPermissionsRepository userPermissionsRepository;

    @Value("${yggdrasil.APILocation}")
    private String APILocation;

    public UserInfoApi(NyanIDuserRepository nyanIDuserRepository, AccountsRepository accountsRepository, UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, UserPermissionsRepository userPermissionsRepository) {
        this.nyanIDuserRepository = nyanIDuserRepository;
        this.accountsRepository = accountsRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.userPermissionsRepository = userPermissionsRepository;
    }


    @GetMapping(produces = "application/json")
    public Object GETMethod(HttpServletResponse response, HttpServletRequest request) {
            String Authorization = request.getHeader("Authorization");
            String Token = Authorization.replace("Bearer ", "").replace(" ", "");
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
                jsonObject.put("akey",userPermissions.getAdminKey());
                jsonObject.put("Aaction",userPermissions.getAction());
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
