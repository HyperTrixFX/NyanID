package moe.koseirin.nyanruaineo.utils;

/*
 * @author KoseiRin_
 * awa
 */

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 用户口令散列工具。
 * <p>
 * 新散列使用 PBKDF2-HMAC-SHA256（随机盐 + 高迭代次数，JDK 内置，无第三方依赖）；
 * 旧散列为 {@code HMAC-SHA256(encryptionKey, password)}（无盐、快速、密钥硬编码，见 C1）。
 * 为平滑迁移，{@link #matches} 兼容旧散列，登录成功后由调用方透明重散列为新格式。
 */
@Component
public class PasswordHasher {

    private static final String PBKDF2_PREFIX = "pbkdf2_sha256$";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${NyanidSetting.encryptionKey:}")
    private String encryptionKey;

    /** 生成 PBKDF2 格式的散列：{@code pbkdf2_sha256$iterations$salt$hash}。 */
    public String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] dk = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BITS);
        return PBKDF2_PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(dk);
    }

    /** 校验口令；旧格式（非 pbkdf2 前缀）按旧 HMAC 做常量时间比较。 */
    public boolean matches(String password, String stored) {
        if (password == null || stored == null || stored.isEmpty()) {
            return false;
        }
        if (stored.startsWith(PBKDF2_PREFIX)) {
            return verifyPbkdf2(password, stored);
        }
        String legacy = legacyHmac(password);
        return legacy != null && MessageDigest.isEqual(
                legacy.getBytes(StandardCharsets.UTF_8),
                stored.getBytes(StandardCharsets.UTF_8));
    }

    /** 该散列是否为旧 HMAC 格式（需要迁移）。 */
    public boolean isLegacy(String stored) {
        return stored == null || !stored.startsWith(PBKDF2_PREFIX);
    }

    private boolean verifyPbkdf2(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 failed", e);
        }
    }

    private String legacyHmac(String password) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return null;
        }
    }
}
