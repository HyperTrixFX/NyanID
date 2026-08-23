package moe.koseirin.nyanruaineo.network.Minecraft.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.network.Minecraft.protocol.packet.LoginSuccess;
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

/**
 * Performs the Mojang session-server authentication used by online mode. Mirrors BungeeCord's
 * session query: it derives the server hash from the shared secret and public key, then asks the
 * session server whether the player joined.
 */
@Slf4j
@Service
public class MojangAuthService {

    private static final String AUTH_URL = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final OkHttpClient CLIENT = new OkHttpClient();

    public CompletableFuture<PlayerProfile> authenticate(String username, byte[] sharedSecret, byte[] publicKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String serverId = generateServerHash(sharedSecret, publicKey);
                String url = AUTH_URL + "?username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                        + "&serverId=" + URLEncoder.encode(serverId, StandardCharsets.UTF_8);
                log.debug("Mojang auth URL: {}", url);

                Request request = new Request.Builder().url(url).get().build();
                try (Response response = CLIENT.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        throw new RuntimeException("Authentication failed: HTTP " + response.code());
                    }

                    JSONObject json = JSON.parseObject(response.body().string());
                    if (json == null || !json.containsKey("id")) {
                        throw new RuntimeException("Invalid response from Mojang");
                    }

                    String uuidStr = json.getString("id");
                    String name = json.getString("name");

                    List<LoginSuccess.Property> properties = new ArrayList<>();
                    JSONArray propertiesArray = json.getJSONArray("properties");
                    if (propertiesArray != null) {
                        for (int i = 0; i < propertiesArray.size(); i++) {
                            JSONObject prop = propertiesArray.getJSONObject(i);
                            String propName = prop.getString("name");
                            String propValue = prop.getString("value");
                            String signature = prop.containsKey("signature") ? prop.getString("signature") : null;
                            properties.add(new LoginSuccess.Property(propName, propValue, signature));
                        }
                    }

                    UUID uuid = UUID.fromString(uuidStr.replaceFirst(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                            "$1-$2-$3-$4-$5"));

                    log.info("Authentication successful for {} ({}) with {} properties", name, uuid, properties.size());
                    return new PlayerProfile(name, uuid, properties);
                }
            } catch (Exception e) {
                throw new RuntimeException("Authentication failed", e);
            }
        });
    }

    private String generateServerHash(byte[] sharedSecret, byte[] publicKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(sharedSecret);
        digest.update(publicKey);
        byte[] hash = digest.digest();
        // The server id is the SHA-1 hash in a signed-hex form (no leading zero padding).
        return new BigInteger(hash).toString(16);
    }

    public record PlayerProfile(String name, UUID uuid, List<LoginSuccess.Property> properties) {
    }
}
