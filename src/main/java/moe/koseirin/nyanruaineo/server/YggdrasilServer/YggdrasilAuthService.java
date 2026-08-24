package moe.koseirin.nyanruaineo.server.YggdrasilServer;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.entity.Yggdrasil;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.repository.YggdrasilPlayerRepository;
import moe.koseirin.nyanruaineo.repository.YggdrasilRepository;
import moe.koseirin.nyanruaineo.server.YggdrasilServer.Authserver.Json.TexturesJson;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * The internal (non-HTTP) Yggdrasil session verification shared by the REST sessionserver
 * endpoint and the proxy's login flow. It looks the {@code serverId} up in the Redis join
 * sessions, validates the access token and player name, and builds the profile JSON with the
 * signed textures — exactly the data the Yggdrasil protocol returns, but as a direct method call
 * instead of an HTTP round trip.
 */
@Slf4j
@Service
public class YggdrasilAuthService {

    private final YggdrasilRepository yggdrasilRepository;
    private final YggdrasilPlayerRepository yggdrasilPlayerRepository;
    private final UserDevicesRepository userDevicesRepository;
    private final RedisService redisService;
    private final utilset utilset;

    @Value("${yggdrasil.APILocation}")
    private String APILocation;

    @Value("${yggdrasil.privateKey}")
    private String privateKey;

    public YggdrasilAuthService(YggdrasilRepository yggdrasilRepository,
                                YggdrasilPlayerRepository yggdrasilPlayerRepository,
                                UserDevicesRepository userDevicesRepository,
                                RedisService redisService,
                                utilset utilset) {
        this.yggdrasilRepository = yggdrasilRepository;
        this.yggdrasilPlayerRepository = yggdrasilPlayerRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.redisService = redisService;
        this.utilset = utilset;
    }

    /**
     * Verifies a join session without any HTTP call. Returns the profile JSON
     * ({@code id}/{@code name}/{@code properties}) when the session is valid, or {@code null}
     * when it is missing/invalid — which is also how the proxy auto-detects that a client did
     * NOT authenticate against this Yggdrasil server.
     */
    public JSONObject hasJoined(String username, String serverId) {
        if (username == null || serverId == null) {
            return null;
        }
        try {
            Object sessionObj = redisService.getValue(serverId);
            if (sessionObj == null) {
                return null;
            }
            redisService.deleteValue(serverId);
            JSONObject sessionData = JSONObject.parseObject(sessionObj.toString());
            if (sessionData == null) {
                return null;
            }

            String accessToken = sessionData.getString("accessToken");
            String nuid = userDevicesRepository.findUidByToken(accessToken);
            if (nuid == null) {
                return null;
            }

            String mcUuid = yggdrasilRepository.GetPlayerUUID(nuid);
            String mcName = yggdrasilRepository.GetPlayerNAME(nuid);
            if (mcUuid == null || !username.equals(mcName)) {
                return null;
            }

            Yggdrasil yggdrasil = yggdrasilRepository.YggdrasilPlayer(mcUuid);
            String model = (yggdrasilPlayerRepository.getSkinTexturesType(mcUuid) == 1) ? "default" : "slim";

            JSONObject texturesJson = new JSONObject();
            texturesJson.put("timestamp", System.currentTimeMillis());
            texturesJson.put("profileId", mcUuid.replace("-", ""));
            texturesJson.put("profileName", mcName);
            texturesJson.put("signatureRequired", true);
            JSONObject textures = new JSONObject();
            texturesJson.put("textures", textures);

            if (yggdrasil.getUseSkin()) {
                TexturesJson.SkinTexture skin = new TexturesJson.SkinTexture(
                        APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getSkinTexturesHash(yggdrasil.getUuid()),
                        new TexturesJson.TextureMetadata(model));
                textures.put("SKIN", skin);
            }
            if (yggdrasil.getUseCAPE()) {
                TexturesJson.SkinTexture cape = new TexturesJson.SkinTexture(
                        APILocation + "/api/zako/res/textures/" + yggdrasilPlayerRepository.getCAPETexturesHash(yggdrasil.getUuid()),
                        null);
                textures.put("CAPE", cape);
            }

            byte[] textureBytes = Base64.getEncoder().encode(texturesJson.toString().getBytes());
            String sign = utilset.sign(textureBytes, privateKey);

            JSONArray properties = new JSONArray();
            JSONObject texturesProperty = new JSONObject();
            texturesProperty.put("name", "textures");
            texturesProperty.put("value", Base64.getEncoder().encodeToString(texturesJson.toString().getBytes()));
            texturesProperty.put("signature", sign);
            properties.add(texturesProperty);

            JSONObject profile = new JSONObject();
            profile.put("id", mcUuid.replace("-", ""));
            profile.put("name", mcName);
            profile.put("properties", properties);
            return profile;
        } catch (Exception e) {
            log.warn("Internal Yggdrasil hasJoined failed for {}: {}", username, e.getMessage());
            return null;
        }
    }
}
