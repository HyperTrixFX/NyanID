package moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver;


import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.CharacterInformationJson;
import moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.Property;
import moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.OtherUtils;
import moe.takanashihoshino.nyaniduserserver.utils.RedisUtils.RedisService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.*;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.UserDevicesService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.UserDevices;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Yggdrasil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("api/yggdrasil/authserver")
public class Authserver {


    private final AccountsRepository accountsRepository;

    private final UserDevicesService userDevicesService;

    private final UserDevicesRepository userDevicesRepository;

    private final YggdrasilRepository yggdrasilRepository;

    private final YggdrasilPlayerRepository yggdrasilPlayerRepository;

    private final BanUserRepository banUserRepository;

    private final RedisService redisService;

    @Value("${NyanidSetting.encryptionKey}")
    private String encryptionKey;
    @Value("${yggdrasil.APILocation}")
    private String APILocation;
    @Value("${yggdrasil.privateKey}")
    private String  privateKey;
    private final Map<String, Authserver.Const> constMap = new HashMap<>();
    public  String EventID = "LoEvent1";

    public Authserver(AccountsRepository accountsRepository, UserDevicesService userDevicesService, UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, YggdrasilPlayerRepository yggdrasilPlayerRepository, BanUserRepository banUserRepository, RedisService redisService) {
        this.accountsRepository = accountsRepository;
        this.userDevicesService = userDevicesService;
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.yggdrasilPlayerRepository = yggdrasilPlayerRepository;
        this.banUserRepository = banUserRepository;
        this.redisService = redisService;
    }


    @PostMapping("authenticate")
    public <T> Object Authenticate(@RequestBody(required = false) T data,HttpServletResponse response, HttpServletRequest request) throws Exception {
        if (data != null){
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
            if (json.containsKey("username") && json.containsKey("password")){
                if (json.containsKey("requestUser")){
                    if (json.containsKey("agent") && json.getString("username").matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")) {
                            String email = json.getString("username");
                            String password = json.getString("password");
                            String clientToken = json.getString("clientToken");
                            String IP = request.getRemoteAddr();
                            String ClientToken;
                            Boolean requestUser = json.getBoolean("requestUser");
                            JSONObject agent = json.getJSONObject("agent");
                            String name = "McDef",version = "0.1";
                            if (agent.containsKey("name") && agent.containsKey("version")){
                            String n = agent.getString("name");
                            String v = agent.getString("version");
                            name = n;
                            version = v;
                            }
                        JSONObject BanEvent = new JSONObject();
                        BanEvent.put(EventID, email);
                        if (accountsRepository.findByEmail(email) != null) {
                            if (redisService.getValue(String.valueOf(BanEvent)) != null && redisService.getValue(String.valueOf(BanEvent)).equals(IP)) {
                                return ErrRes.NotFoundAccountException("The account doesn't exist or is locked because of a password error 杂鱼喵~", response);
                            } else {
                                if (constMap.get(email) == null) {
                                    constMap.put(email, new Authserver.Const(1));
                                } else if (constMap.get(email).requestCount > 3) {
                                    constMap.remove(email);
                                    redisService.setValueWithExpiration(String.valueOf(BanEvent), IP, 180, TimeUnit.SECONDS);
                                }
                                String pwd = accountsRepository.LoginByEmail(email);
                                String lockpwd = OtherUtils.HMACSHA256(encryptionKey,password);
                                if (Objects.equals(lockpwd, pwd)) {
                                    String uid = accountsRepository.findByEmail(email);
                                    String MCUUID = yggdrasilRepository.GetPlayerUUID(uid);
                                    if (MCUUID != null){
                                        String MCNAME = yggdrasilRepository.GetPlayerNAME(MCUUID);
                                    String session = request.getSession().getId();
                                    if (banUserRepository.findBanIDByUid(uid) == null) {
                                        if (constMap.get(email) != null) {
                                            constMap.remove(email);
                                        }
                                        //返回登录信息
                                        if (json.containsKey("clientToken")) {
                                            if (clientToken.isEmpty()) {
                                                ClientToken = OtherUtils.RandomString(32);
                                                String accessToken = OtherUtils.RandomString(32);
                                                UserDevices userDevices = new UserDevices();
                                                userDevices.setUid(uid);
                                                userDevices.setDeviceID(name+".Td"+version+"-Lo.-" + MCUUID);
                                                userDevices.setDeviceName("Minecraft");
                                                userDevices.setToken(accessToken);
                                                userDevices.setIp(IP);
                                                userDevices.setIsActive(true);
                                                userDevices.setSession(session);
                                                userDevices.setClientid(ClientToken);
                                                userDevices.setCreateTime(LocalDateTime.now());
                                                userDevicesService.save(userDevices);
                                                return Response(MCUUID, MCNAME, accessToken, ClientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID),requestUser,uid);
                                            } else {
                                                if (json.getString("clientToken").length() == 32) {
                                                    if (userDevicesRepository.getByINFO(clientToken) == null) {
                                                        String accessToken = OtherUtils.RandomString(32);
                                                        UserDevices uD = new UserDevices();
                                                        uD.setUid(uid);
                                                        uD.setDeviceID("Mc.Td-LoToken.-" + MCUUID);
                                                        uD.setDeviceName("Minecraft");
                                                        uD.setToken(accessToken);
                                                        uD.setIp(IP);
                                                        uD.setIsActive(true);
                                                        uD.setSession(session);
                                                        uD.setClientid(clientToken);
                                                        uD.setCreateTime(LocalDateTime.now());
                                                        userDevicesService.save(uD);
                                                        return Response(MCUUID, MCNAME, accessToken, clientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID), requestUser, uid);
                                                    } else {
                                                        UserDevices D = userDevicesRepository.getByINFO(clientToken);
                                                        return Response(MCUUID, MCNAME, D.getToken(), clientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID), requestUser, uid);
                                                    }
                                                }else {
                                                    return ErrRes.YggdrasilError("非法clientToken长度,请尝试更换兼容启动器登录杂鱼喵!","ForbiddenOperationException","Invalid clientToken.",403,response);
                                                }
                                            }
                                        }else {
                                            ClientToken = OtherUtils.RandomString(32);
                                            String accessToken = OtherUtils.RandomString(32);
                                            UserDevices userDevices = new UserDevices();
                                            userDevices.setUid(uid);
                                            userDevices.setDeviceID("Mc.Td-LoToken.-" + MCUUID);
                                            userDevices.setDeviceName("Minecraft");
                                            userDevices.setToken(accessToken);
                                            userDevices.setIp(IP);
                                            userDevices.setIsActive(true);
                                            userDevices.setSession(session);
                                            userDevices.setClientid(ClientToken);
                                            userDevices.setCreateTime(LocalDateTime.now());
                                            userDevicesService.save(userDevices);
                                            return Response(MCUUID, MCNAME, accessToken, ClientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID),requestUser,uid);
                                        }
                                    }else {
                                        return ErrRes.YggdrasilError("此用户已被封禁,封禁码:["+banUserRepository.findBanIDByUid(uid)+"]杂鱼喵~", "ForbiddenOperationException","ForbiddenOperationException",403,response);
                                    }
                                    } else {
                                        return ErrRes.YggdrasilError("The Yggdrasil account doesn't exist . 杂鱼喵~ ","Not Found ","Not Found Yggdrasil account ",404, response);
                                    }
                                } else {
                                    if (constMap.get(email) != null) {
                                        constMap.get(email).requestCount++;
                                    }
                                    return ErrRes.YggdrasilError("The account doesn't exist or is locked because of a password error 杂鱼喵~", "ForbiddenOperationException","Invalid credentials. Invalid username or password.",403,response);
                                }
                            }
                        } else {
                            return ErrRes.YggdrasilError("The account doesn't exist or is locked because of a password error 杂鱼喵~", "ForbiddenOperationException","Invalid credentials. Invalid username or password.",403,response);
                        }
                }else {
                        return ErrRes.YggdrasilError("你请求的json中缺少重要参数agent杂鱼喵~","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
                    }
            }else {
                    return ErrRes.YggdrasilError("The requestUser or clientToken is incorrect 杂鱼喵~","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
                }
        }else {
                return ErrRes.YggdrasilError("你请求的json中缺少重要参数username或password杂鱼喵~","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
            }
        }else {
            return ErrRes.YggdrasilError("你请求的内容为NULL杂鱼喵!","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
        }
    }

    @PostMapping("refresh")
    public <T> Object Refresh(@RequestBody(required = false) T data,HttpServletResponse response, HttpServletRequest request) throws Exception {
        if (data != null) {
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
            if (json.containsKey("accessToken")){
                if (json.containsKey("requestUser") ){
                    Boolean IsSelectedProfile = json.containsKey("selectedProfile");
                    //
                    if (json.containsKey("clientToken")){
                        if (json.getString("clientToken").isEmpty()){
                            //未指定clientToken
                            if (userDevicesRepository.findClientIdByToken(json.getString("accessToken")) != null){
                                userDevicesRepository.UpdateCreateTime(LocalDateTime.now(),json.getString("accessToken"));
                                String clientToken = userDevicesRepository.findClientIdByToken(json.getString("accessToken"));
                                String nyanid = userDevicesRepository.findUidByToken(json.getString("accessToken"));
                                String accessToken = OtherUtils.RandomString(32);
                                userDevicesRepository.UpdateAccessToken(json.getString("accessToken"),accessToken);
                                Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(nyanid);
                                return RefreshResponse(IsSelectedProfile,json.getBoolean("requestUser"),accessToken,clientToken,yggdrasil.getPlayername(),yggdrasil.getUuid(),nyanid);
                            }else {
                                //错误accessToken
                                return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid token.",403,response);
                            }
                        }else {
                            //指定clientToken
                            if (userDevicesRepository.findClientIdByToken(json.getString("accessToken")) != null){
                                String clientToken = userDevicesRepository.findClientIdByToken(json.getString("accessToken"));
                                if (clientToken.equals(json.getString("clientToken"))){
                                    userDevicesRepository.UpdateCreateTime(LocalDateTime.now(),json.getString("accessToken"));
                                    String nyanid = userDevicesRepository.findUidByToken(json.getString("accessToken"));
                                    String accessToken = OtherUtils.RandomString(32);
                                    userDevicesRepository.UpdateAccessToken(json.getString("accessToken"),accessToken);
                                    Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(nyanid);
                                    return RefreshResponse(IsSelectedProfile,json.getBoolean("requestUser"),accessToken,clientToken,yggdrasil.getPlayername(),yggdrasil.getUuid(),nyanid);
                                }else {
                                    return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid clientToken.",403,response);
                                }
                            }else {
                                //错误accessToken
                                return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid token.",403,response);
                            }
                        }
                    }else {
                        //未指定clientToken
                        if (userDevicesRepository.findClientIdByToken(json.getString("accessToken")) != null){
                            userDevicesRepository.UpdateCreateTime(LocalDateTime.now(),json.getString("accessToken"));
                            String clientToken = userDevicesRepository.findClientIdByToken(json.getString("accessToken"));
                            String nyanid = userDevicesRepository.findUidByToken(json.getString("accessToken"));
                            String accessToken = OtherUtils.RandomString(32);
                            userDevicesRepository.UpdateAccessToken(json.getString("accessToken"),accessToken);
                            Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(nyanid);
                            return RefreshResponse(IsSelectedProfile,json.getBoolean("requestUser"),accessToken,clientToken,yggdrasil.getPlayername(),yggdrasil.getUuid(),nyanid);
                        }else {
                            //错误accessToken
                            return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid token.",403,response);
                        }
                    }
                }else {
                    return ErrRes.YggdrasilError("你请求的json中缺少重要参数requestUser杂鱼喵!","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
                }
            }else {
                return ErrRes.YggdrasilError("你请求的json中缺少重要参数accessToken杂鱼喵!","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
            }
        }else {
            return ErrRes.YggdrasilError("你请求的内容为NULL杂鱼喵!","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
        }
    }

    @PostMapping("validate")
    public <T> Object Validate(@RequestBody(required = false) T data,HttpServletResponse response, HttpServletRequest request){
        if (data != null) {
            JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
            if (json.containsKey("accessToken")){
                    //
                    if (json.containsKey("clientToken")){
                        if (json.getString("clientToken").isEmpty()){
                            //未指定clientToken
                            if (userDevicesRepository.findClientIdByToken(json.getString("accessToken")) != null&& userDevicesRepository.getActive(json.getString("accessToken"))){
                                response.setStatus(204);
                                return null;
                            }else {
                                //错误accessToken
                                return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid token.",403,response);
                            }
                        }else {
                            //指定clientToken
                            if (userDevicesRepository.findClientIdByToken(json.getString("accessToken")) != null){
                                String clientToken = userDevicesRepository.findClientIdByToken(json.getString("accessToken"));
                                if (clientToken.equals(json.getString("clientToken")) && userDevicesRepository.getActive(json.getString("accessToken"))){
                                    response.setStatus(204);
                                    return null;
                                }else {
                                    return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid clientToken.",403,response);
                                }
                            }else {
                                //错误accessToken
                                return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid token.",403,response);
                            }
                        }
                    }else {
                        //未指定clientToken
                        if (userDevicesRepository.findClientIdByToken(json.getString("accessToken")) != null&& userDevicesRepository.getActive(json.getString("accessToken"))){
                            response.setStatus(204);
                            return null;
                        }else {
                            //错误accessToken
                            return ErrRes.YggdrasilError("登录信息已过期杂鱼喵!","ForbiddenOperationException","Invalid token.",403,response);
                        }
                    }
            }else {
                return ErrRes.YggdrasilError("你请求的json中缺少重要参数accessToken杂鱼喵!","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
            }
        }else {
            return ErrRes.YggdrasilError("你请求的内容为NULL杂鱼喵!","The parameter is incorrect","The parameter is incorrect 杂鱼喵~",403,response);
        }
    }

    private Object Response(String MCUUID,String MCNAME,String accessToken,String ClientToken,int SkinType,Boolean requestUser,String nyanid) throws Exception {
        String MODEL;
        // 创建 SKIN 纹理
        if (SkinType == 1){
            MODEL =  "default";
        }else {
            MODEL =  "slim";
        }
        JSONObject TexturesJson = new JSONObject();
        TexturesJson.put("timestamp",System.currentTimeMillis());
        TexturesJson.put("profileId",MCUUID.replace("-",""));
        TexturesJson.put("profileName",MCNAME);
        TexturesJson.putObject("textures");
        JSONObject textures = TexturesJson.putObject("textures");
        // 2. 处理 SKIN
        if (yggdrasilRepository.getUseSkin(MCUUID)) {
            moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson.TextureMetadata d = new TexturesJson.TextureMetadata(MODEL);
            moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson.SkinTexture s = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getSkinTexturesHash(MCUUID), d);

            // 直接放入统一的 textures 对象
            textures.put("SKIN", s);
        }
        // 3. 处理 CAPE
        if (yggdrasilRepository.getUseCAPE(MCUUID)) {
            moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson.SkinTexture d = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getCAPETexturesHash(MCUUID), null);

            // 直接放入统一的 textures 对象
            textures.put("CAPE", d);
        }
        // 1. 创建示例数据
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("textures", Base64.getEncoder().encodeToString(TexturesJson.toString().getBytes()), null));


        CharacterInformationJson characterInformationJson = new CharacterInformationJson(
                MCUUID.replace("-",""),
                yggdrasilRepository.GetPlayerNAME(MCUUID),
                properties
        );
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("accessToken",accessToken);
        jsonObject.put("clientToken",ClientToken);
        jsonObject.putArray("availableProfiles").add(characterInformationJson);
        jsonObject.putObject("selectedProfile");
        jsonObject.put("selectedProfile",characterInformationJson);
        if (requestUser){
            JSONObject Properties = new JSONObject();
            Properties.put("name","preferredLanguage");
            Properties.put("value","zh_CN");
            JSONObject user = new JSONObject();
            user.put("id",nyanid);
            user.putArray("properties").add(Properties);
            jsonObject.put("user",user);
        }
        return jsonObject;
    }

    private Object RefreshResponse(boolean IsSelectedProfile,boolean requestUser, String accessToken,String clientToken,String name, String uuid,String nyanid) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("accessToken",accessToken);
        jsonObject.put("clientToken",clientToken);
        JSONObject TexturesJson = new JSONObject();
        TexturesJson.put("timestamp",System.currentTimeMillis());
        TexturesJson.put("profileId",uuid.replace("-",""));
        TexturesJson.put("profileName",yggdrasilRepository.GetPlayerNAME(uuid));
        TexturesJson.putObject("textures");
        String MODEL;
        // 创建 SKIN 纹理
        if (yggdrasilPlayerRepository.getSkinTexturesType(uuid) == 1){
            MODEL =  "default";
        }else {
            MODEL =  "slim";
        }
        JSONObject textures = TexturesJson.putObject("textures");
        // 2. 处理 SKIN
        if (yggdrasilRepository.getUseSkin(uuid)) {
            moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson.TextureMetadata d = new TexturesJson.TextureMetadata(MODEL);
            moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson.SkinTexture s = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getSkinTexturesHash(uuid), d);

            // 直接放入统一的 textures 对象
            textures.put("SKIN", s);
        }
        // 3. 处理 CAPE
        if (yggdrasilRepository.getUseCAPE(uuid)) {
            moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer.Authserver.Json.TexturesJson.SkinTexture d = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getCAPETexturesHash(uuid), null);

            // 直接放入统一的 textures 对象
            textures.put("CAPE", d);
        }
        if (IsSelectedProfile){
            List<Property> properties = new ArrayList<>();
            properties.add(new Property("textures", Base64.getEncoder().encodeToString(TexturesJson.toString().getBytes()),null));
            CharacterInformationJson characterInformationJson = new CharacterInformationJson(
                    uuid.replace("-",""),
                    name,
                    properties
            );
            jsonObject.putObject("selectedProfile");
            jsonObject.put("selectedProfile",characterInformationJson);
        }
        if (requestUser){
            JSONObject Properties = new JSONObject();
            Properties.put("name","preferredLanguage");
            Properties.put("value","zh_CN");
            JSONObject user = new JSONObject();
            user.put("id",nyanid);
            user.putArray("properties").add(Properties);
            jsonObject.put("user",user);
        }
        return jsonObject;
    }


    private static class Const {
        int requestCount;
        Const(int requestCount) {
            this.requestCount = requestCount;
        }
    }
}