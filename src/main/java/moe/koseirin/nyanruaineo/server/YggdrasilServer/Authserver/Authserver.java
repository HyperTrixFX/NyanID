package moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver;


/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.repository.*;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver.Json.CharacterInformationJson;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver.Json.Property;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver.Json.TexturesJson;
import moe.koseirin.nyanruaineo.utils.ErrorUtils.ErrorResponse;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.SqlService.UserDevicesService;
import moe.koseirin.nyanruaineo.entity.UserDevices;
import moe.koseirin.nyanruaineo.entity.Yggdrasil;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.WebMvc.StrictIpResolver;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final utilset utilset;
    private final StrictIpResolver strictIpResolver;
    private final Respond respond;

    @Value("${NyanidSetting.encryptionKey}")
    private String encryptionKey;
    @Value("${yggdrasil.APILocation}")
    private String APILocation;
    @Value("${yggdrasil.privateKey}")
    private String privateKey;
    @Value("${yggdrasil.publicKey}")
    private String publicKey;
    private final Map<String, Authserver.Const> constMap = new HashMap<>();
    public String EventID = "LoEvent1";

    public Authserver(AccountsRepository accountsRepository, UserDevicesService userDevicesService, UserDevicesRepository userDevicesRepository, YggdrasilRepository yggdrasilRepository, YggdrasilPlayerRepository yggdrasilPlayerRepository, BanUserRepository banUserRepository, RedisService redisService, utilset utilset, StrictIpResolver strictIpResolver, Respond respond) {
        this.accountsRepository = accountsRepository;
        this.userDevicesService = userDevicesService;
        this.userDevicesRepository = userDevicesRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.yggdrasilPlayerRepository = yggdrasilPlayerRepository;
        this.banUserRepository = banUserRepository;
        this.redisService = redisService;
        this.utilset = utilset;
        this.strictIpResolver = strictIpResolver;
        this.respond = respond;
    }

    @PostMapping("authenticate")
    public ResponseEntity<?> Authenticate(@RequestBody(required = false) String data, HttpServletRequest request) {
        if (data == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的内容为NULL杂鱼喵!", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }
        JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
        if (!json.containsKey("username") || !json.containsKey("password")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的json中缺少重要参数username或password杂鱼喵~", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }
        if (!json.containsKey("requestUser")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("The requestUser or clientToken is incorrect 杂鱼喵~", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }
        if (!json.containsKey("agent") || !json.getString("username").matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的json中缺少重要参数agent杂鱼喵~", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }

        String email = json.getString("username");
        String password = json.getString("password");
        String clientToken = json.getString("clientToken");
        String IP = strictIpResolver.getStrictClientIp(request);
        String ClientToken;
        Boolean requestUser = json.getBoolean("requestUser");
        JSONObject agent = json.getJSONObject("agent");
        String name = "McDef", version = "0.1";
        if (agent.containsKey("name") && agent.containsKey("version")) {
            name = agent.getString("name");
            version = agent.getString("version");
        }

        JSONObject BanEvent = new JSONObject();
        BanEvent.put(EventID, email);
        if (accountsRepository.findByEmail(email) == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("The account doesn't exist or is locked because of a password error 杂鱼喵~", "ForbiddenOperationException", "Invalid credentials. Invalid username or password."));
        }

        if (redisService.getValue(String.valueOf(BanEvent)) != null && redisService.getValue(String.valueOf(BanEvent)).equals(IP)) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("The account doesn't exist or is locked because of a password error 杂鱼喵~", "ForbiddenOperationException", "Invalid credentials. Invalid username or password."));
        }

        if (constMap.get(email) == null) {
            constMap.put(email, new Authserver.Const(1));
        } else if (constMap.get(email).requestCount > 3) {
            constMap.remove(email);
            redisService.setValueWithExpiration(String.valueOf(BanEvent), IP, 180, TimeUnit.SECONDS);
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("The account doesn't exist or is locked because of a password error 杂鱼喵~", "ForbiddenOperationException", "Invalid credentials. Invalid username or password."));
        }

        String pwd = accountsRepository.LoginByEmail(email);
        String lockpwd = utilset.HMACSHA256(encryptionKey, password);
        if (!Objects.equals(lockpwd, pwd)) {
            if (constMap.get(email) != null) {
                constMap.get(email).requestCount++;
            }
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("The account doesn't exist or is locked because of a password error 杂鱼喵~", "ForbiddenOperationException", "Invalid credentials. Invalid username or password."));
        }

        String uid = accountsRepository.findByEmail(email);
        String MCUUID = yggdrasilRepository.GetPlayerUUID(uid);
        if (MCUUID == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 404, new ErrorResponse("The Yggdrasil account doesn't exist . 杂鱼喵~ ", "Not Found ", "Not Found Yggdrasil account "));
        }

        String MCNAME = yggdrasilRepository.GetPlayerNAME(MCUUID);
        String session = request.getSession().getId();
        if (banUserRepository.findBanIDByUid(uid) != null) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("此用户已被封禁,封禁码:[" + banUserRepository.findBanIDByUid(uid) + "]杂鱼喵~", "ForbiddenOperationException", "ForbiddenOperationException"));
        }

        if (constMap.get(email) != null) {
            constMap.remove(email);
        }
        //返回登录信息
        if (json.containsKey("clientToken")) {
            if (clientToken == null || clientToken.isEmpty()) {
                ClientToken = utilset.RandomString(32);
                String accessToken = utilset.RandomString(32);
                UserDevices userDevices = new UserDevices();
                userDevices.setUid(uid);
                userDevices.setDeviceID(name + ".Td" + version + "-Lo.-" + MCUUID);
                userDevices.setDeviceName("Minecraft");
                userDevices.setToken(accessToken);
                userDevices.setIp(IP);
                userDevices.setIsActive(true);
                userDevices.setSession(session);
                userDevices.setClientId(ClientToken);
                userDevices.setCreateTime(LocalDateTime.now());
                userDevicesService.save(userDevices);
                return respond.respond(MediaType.APPLICATION_JSON,200,Response(MCUUID, MCNAME, utilset.encrypt(accessToken, publicKey), ClientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID), requestUser, uid));
            } else {
                if (clientToken.length() == 32) {
                    UserDevices existing = userDevicesRepository.getByINFO(clientToken);
                    if (existing == null) {
                        String accessToken = utilset.RandomString(32);
                        UserDevices uD = new UserDevices();
                        uD.setUid(uid);
                        uD.setDeviceID("Mc.Td-LoToken.-" + MCUUID);
                        uD.setDeviceName("Minecraft");
                        uD.setToken(accessToken);
                        uD.setIp(IP);
                        uD.setIsActive(true);
                        uD.setSession(session);
                        uD.setClientId(clientToken);
                        uD.setCreateTime(LocalDateTime.now());
                        userDevicesService.save(uD);
                        return respond.respond(MediaType.APPLICATION_JSON,200,(Response(MCUUID, MCNAME, utilset.encrypt(accessToken, publicKey), clientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID), requestUser, uid)));
                    } else {
                        return respond.respond(MediaType.APPLICATION_JSON,200,(Response(MCUUID, MCNAME, utilset.encrypt(existing.getToken(), publicKey), clientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID), requestUser, uid)));
                    }
                } else {
                    return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("非法clientToken长度,请尝试更换兼容启动器登录杂鱼喵!", "ForbiddenOperationException", "Invalid clientToken."));
                }
            }
        } else {
            ClientToken = utilset.RandomString(32);
            String accessToken = utilset.RandomString(32);
            UserDevices userDevices = new UserDevices();
            userDevices.setUid(uid);
            userDevices.setDeviceID("Mc.Td-LoToken.-" + MCUUID);
            userDevices.setDeviceName("Minecraft");
            userDevices.setToken(accessToken);
            userDevices.setIp(IP);
            userDevices.setIsActive(true);
            userDevices.setSession(session);
            userDevices.setClientId(ClientToken);
            userDevices.setCreateTime(LocalDateTime.now());
            userDevicesService.save(userDevices);
            return respond.respond(MediaType.APPLICATION_JSON,200,(Response(MCUUID, MCNAME, utilset.encrypt(accessToken, privateKey), ClientToken, yggdrasilPlayerRepository.getSkinTexturesType(MCUUID), requestUser, uid)));
        }
    }

    @PostMapping("refresh")
    public ResponseEntity<?> Refresh(@RequestBody(required = false) String data) {
        if (data == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的内容为NULL杂鱼喵!", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }
        JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
        if (!json.containsKey("accessToken")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的json中缺少重要参数accessToken杂鱼喵!", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }
        if (!json.containsKey("requestUser")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的json中缺少重要参数requestUser杂鱼喵!", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }

        String decryptedToken = utilset.decrypt(json.getString("accessToken"), privateKey);
        boolean IsSelectedProfile = json.containsKey("selectedProfile");

        if (json.containsKey("clientToken")) {
            String reqClientToken = json.getString("clientToken");
            if (reqClientToken == null || reqClientToken.isEmpty()) {
                // 未指定clientToken
                String existingClient = userDevicesRepository.findClientIdByToken(decryptedToken);
                if (existingClient == null) {
                    return respond.respond(MediaType.APPLICATION_JSON, 403,
                            new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid token."));
                }
                userDevicesRepository.UpdateCreateTime(LocalDateTime.now(), decryptedToken);
                String nyanid = userDevicesRepository.findUidByToken(decryptedToken);
                String newAccessToken = utilset.RandomString(32);
                userDevicesRepository.UpdateAccessToken(decryptedToken, newAccessToken);
                Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(nyanid);
                return respond.respond(MediaType.APPLICATION_JSON,200,(RefreshResponse(IsSelectedProfile, json.getBoolean("requestUser"), utilset.encrypt(newAccessToken, publicKey), existingClient, yggdrasil.getPlayername(), yggdrasil.getUuid(), nyanid)));
            } else {
                // 指定clientToken
                String existingClient = userDevicesRepository.findClientIdByToken(decryptedToken);
                if (existingClient == null) {
                    return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid token."));
                }
                if (!existingClient.equals(reqClientToken)) {
                    return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid clientToken."));
                }
                userDevicesRepository.UpdateCreateTime(LocalDateTime.now(), decryptedToken);
                String nyanid = userDevicesRepository.findUidByToken(decryptedToken);
                String newAccessToken = utilset.RandomString(32);
                userDevicesRepository.UpdateAccessToken(decryptedToken, newAccessToken);
                Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(nyanid);
                return respond.respond(MediaType.APPLICATION_JSON,200,(RefreshResponse(IsSelectedProfile, json.getBoolean("requestUser"), utilset.encrypt(newAccessToken, publicKey), reqClientToken, yggdrasil.getPlayername(), yggdrasil.getUuid(), nyanid)));
            }
        } else {
            // 未指定clientToken
            String existingClient = userDevicesRepository.findClientIdByToken(decryptedToken);
            if (existingClient == null) {
                return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid token."));
            }
            userDevicesRepository.UpdateCreateTime(LocalDateTime.now(), decryptedToken);
            String nyanid = userDevicesRepository.findUidByToken(decryptedToken);
            String newAccessToken = utilset.RandomString(32);
            userDevicesRepository.UpdateAccessToken(decryptedToken, newAccessToken);
            Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(nyanid);
            return respond.respond(MediaType.APPLICATION_JSON,200,(RefreshResponse(IsSelectedProfile, json.getBoolean("requestUser"), utilset.encrypt(newAccessToken, publicKey), existingClient, yggdrasil.getPlayername(), yggdrasil.getUuid(), nyanid)));
        }
    }

    @PostMapping("validate")
    public ResponseEntity<?> Validate(@RequestBody(required = false) String data) {
        if (data == null) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的内容为NULL杂鱼喵!", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }
        JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(data));
        if (!json.containsKey("accessToken")) {
            return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("你请求的json中缺少重要参数accessToken杂鱼喵!", "The parameter is incorrect", "The parameter is incorrect 杂鱼喵~"));
        }

        String decryptedToken = utilset.decrypt(json.getString("accessToken"), privateKey);

        if (json.containsKey("clientToken")) {
            String reqClientToken = json.getString("clientToken");
            if (reqClientToken == null || reqClientToken.isEmpty()) {
                if (userDevicesRepository.findClientIdByToken(decryptedToken) != null && userDevicesRepository.getActive(decryptedToken)) {
                    return ResponseEntity.status(204).build();
                } else {
                    return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid token."));
                }
            } else {
                String existingClient = userDevicesRepository.findClientIdByToken(decryptedToken);
                if (existingClient != null && existingClient.equals(reqClientToken) && userDevicesRepository.getActive(decryptedToken)) {
                    return ResponseEntity.status(204).build();
                } else {
                    return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid clientToken."));
                }
            }
        } else {
            if (userDevicesRepository.findClientIdByToken(decryptedToken) != null && userDevicesRepository.getActive(decryptedToken)) {
                return ResponseEntity.status(204).build();
            } else {
                return respond.respond(MediaType.APPLICATION_JSON, 403, new ErrorResponse("登录信息已过期杂鱼喵!", "ForbiddenOperationException", "Invalid token."));
            }
        }
    }

    private JSONObject Response(String MCUUID, String MCNAME, String accessToken, String ClientToken, int SkinType, Boolean requestUser, String nyanid) {
        String MODEL = (SkinType == 1) ? "default" : "slim";
        JSONObject TexturesJson = new JSONObject();
        TexturesJson.put("timestamp", System.currentTimeMillis());
        TexturesJson.put("profileId", MCUUID.replace("-", ""));
        TexturesJson.put("profileName", MCNAME);
        JSONObject textures = new JSONObject();
        TexturesJson.put("textures", textures);
        if (yggdrasilRepository.getUseSkin(MCUUID)) {
            TexturesJson.TextureMetadata d = new TexturesJson.TextureMetadata(MODEL);
            TexturesJson.SkinTexture s = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getSkinTexturesHash(MCUUID), d);
            textures.put("SKIN", s);
        }
        if (yggdrasilRepository.getUseCAPE(MCUUID)) {
            TexturesJson.SkinTexture d = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getCAPETexturesHash(MCUUID), null);
            textures.put("CAPE", d);
        }
        List<Property> properties = new ArrayList<>();
        properties.add(new Property("textures", Base64.getEncoder().encodeToString(TexturesJson.toString().getBytes()), null));
        CharacterInformationJson characterInformationJson = new CharacterInformationJson(MCUUID.replace("-", ""), yggdrasilRepository.GetPlayerNAME(MCUUID), properties);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("accessToken", accessToken);
        jsonObject.put("clientToken", ClientToken);
        jsonObject.putArray("availableProfiles").add(characterInformationJson);
        jsonObject.put("selectedProfile", characterInformationJson);
        if (requestUser) {
            JSONObject Properties = new JSONObject();
            Properties.put("name", "preferredLanguage");
            Properties.put("value", "zh_CN");
            JSONObject user = new JSONObject();
            user.put("id", nyanid);
            user.putArray("properties").add(Properties);
            jsonObject.put("user", user);
        }
        return jsonObject;
    }

    private JSONObject RefreshResponse(boolean IsSelectedProfile, boolean requestUser, String accessToken, String clientToken, String name, String uuid, String nyanid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("accessToken", accessToken);
        jsonObject.put("clientToken", clientToken);
        String MODEL = (yggdrasilPlayerRepository.getSkinTexturesType(uuid) == 1) ? "default" : "slim";
        JSONObject TexturesJson = new JSONObject();
        TexturesJson.put("timestamp", System.currentTimeMillis());
        TexturesJson.put("profileId", uuid.replace("-", ""));
        TexturesJson.put("profileName", yggdrasilRepository.GetPlayerNAME(uuid));
        JSONObject textures = new JSONObject();
        TexturesJson.put("textures", textures);
        if (yggdrasilRepository.getUseSkin(uuid)) {
            TexturesJson.TextureMetadata d = new TexturesJson.TextureMetadata(MODEL);
            TexturesJson.SkinTexture s = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getSkinTexturesHash(uuid), d);
            textures.put("SKIN", s);
        }
        if (yggdrasilRepository.getUseCAPE(uuid)) {
            TexturesJson.SkinTexture d = new TexturesJson.SkinTexture(APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getCAPETexturesHash(uuid), null);
            textures.put("CAPE", d);
        }
        if (IsSelectedProfile) {
            List<Property> properties = new ArrayList<>();
            properties.add(new Property("textures", Base64.getEncoder().encodeToString(TexturesJson.toString().getBytes()), null));
            CharacterInformationJson characterInformationJson = new CharacterInformationJson(uuid.replace("-", ""), name, properties);
            jsonObject.put("selectedProfile", characterInformationJson);
        }
        if (requestUser) {
            JSONObject Properties = new JSONObject();
            Properties.put("name", "preferredLanguage");
            Properties.put("value", "zh_CN");
            JSONObject user = new JSONObject();
            user.put("id", nyanid);
            user.putArray("properties").add(Properties);
            jsonObject.put("user", user);
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