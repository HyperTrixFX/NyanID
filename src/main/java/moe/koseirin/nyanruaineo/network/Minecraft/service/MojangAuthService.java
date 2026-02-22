package moe.koseirin.nyanruaineo.network.Minecraft.service;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.network.packet.login.LoginSuccessPacket;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
public class MojangAuthService {

    private static final String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";

    public CompletableFuture<PlayerProfile> authenticate(String username, byte[] sharedSecret, byte[] publicKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String serverId = generateServerHash(sharedSecret, publicKey);
                String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
                String encodedServerId = URLEncoder.encode(serverId, StandardCharsets.UTF_8);

                String url = AUTH_URL + "?username=" + encodedUsername + "&serverId=" + encodedServerId;
                log.debug("Mojang auth URL: {}", url);

                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).get().build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        log.error("Mojang auth failed, response code: {}", response.code());
                        throw new RuntimeException("Authentication failed: HTTP " + response.code());
                    }

                    String responseBody = response.body().string();
                    log.debug("Mojang response: {}", responseBody);

                    JSONObject json = JSON.parseObject(responseBody);
                    if (json == null || !json.containsKey("id")) {
                        throw new RuntimeException("Invalid response from Mojang");
                    }

                    String uuidStr = json.getString("id");
                    String name = json.getString("name");

                    // 解析 properties
                    JSONArray propertiesArray = json.getJSONArray("properties");
                    List<LoginSuccessPacket.Property> properties = new ArrayList<>();
                    if (propertiesArray != null) {
                        for (int i = 0; i < propertiesArray.size(); i++) {
                            JSONObject propObj = propertiesArray.getJSONObject(i);
                            String propName = propObj.getString("name");
                            String propValue = propObj.getString("value");
                            boolean signed = propObj.containsKey("signature");
                            String signature = signed ? propObj.getString("signature") : null;
                            properties.add(new LoginSuccessPacket.Property(propName, propValue, signed, signature));
                        }
                    }

                    // 转换为标准 UUID 格式
                    String formattedUuid = uuidStr.replaceFirst(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                            "$1-$2-$3-$4-$5"
                    );
                    UUID uuid = UUID.fromString(formattedUuid);

                    log.info("Authentication successful for {} ({}) with {} properties", name, uuid, properties.size());
                    return new PlayerProfile(name, uuid, properties);
                }
            } catch (Exception e) {
                log.error("Authentication failed", e);
                throw new RuntimeException("Authentication failed", e);
            }
        });
    }

    private String generateServerHash(byte[] sharedSecret, byte[] publicKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(sharedSecret);
        digest.update(publicKey);
        byte[] hash = digest.digest();
        return new BigInteger(hash).toString(16);
    }

    @Getter
    public static class PlayerProfile {
        private final String name;
        private final UUID uuid;
        private final List<LoginSuccessPacket.Property> properties; // 新增 properties

        public PlayerProfile(String name, UUID uuid, List<LoginSuccessPacket.Property> properties) {
            this.name = name;
            this.uuid = uuid;
            this.properties = properties;
        }
    }
}

