package moe.koseirin.nyanruaineo.network.utils;


import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Objects;


/*
 * @author KoseiRin_
 * awa
 */


@Slf4j
public class SM4Util {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ALGORITHM = "SM4";
    private static final String TRANSFORMATION = "SM4/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, "BC");

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(data);

        byte[] result = new byte[IV_LENGTH + encrypted.length];
        System.arraycopy(iv, 0, result, 0, IV_LENGTH);
        System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);
        return result;
    }

    public static byte[] decrypt(byte[] encryptedData, byte[] key) throws Exception {
        if (encryptedData == null || encryptedData.length < IV_LENGTH) {
            log.error("加密数据长度不足，缺少IV或数据不完整喵~");
        }

        SecretKeySpec keySpec = new SecretKeySpec(key, ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, "BC");

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(Objects.requireNonNull(encryptedData), 0, iv, 0, IV_LENGTH);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        return cipher.doFinal(encryptedData, IV_LENGTH, encryptedData.length - IV_LENGTH);
    }
}
