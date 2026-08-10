package moe.koseirin.nyanruaineo.network.utils;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.stereotype.Component;

import java.util.Base64;


@Slf4j
@Component
public class KeyManager {

    private static final String SM4_KEY_CONFIG_KEY = "sm4Key";

    private final SystemConfigCacheService cacheService;

    public KeyManager(SystemConfigCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public byte[] getCurrentKey() {
        String base64Key = cacheService.getConfig(SM4_KEY_CONFIG_KEY);
        try {
            if (base64Key == null) {
                log.error("SM4 key not configured in database (key: " + SM4_KEY_CONFIG_KEY + ")");
                cacheService.addConfig("sm4Key","AQIDBAUGBwgJCgsMDQ4PEA==");
                cacheService.loadConfigs();
            }
            return Base64.getDecoder().decode(base64Key);
        }catch (NullPointerException e){
            cacheService.loadConfigs();
            return null;
        }

    }

    public byte[] getKey(String sessionId) {
        return getCurrentKey();
    }
}