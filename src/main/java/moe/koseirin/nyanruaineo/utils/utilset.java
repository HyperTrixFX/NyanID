package moe.koseirin.nyanruaineo.utils;

/*
 * @author KoseiRin_
 * awa
 */



import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.utils.System.EnumList.UUIDtype;
import moe.koseirin.nyanruaineo.utils.WebMvc.StrictIpResolver;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Blob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;



@Slf4j
@Component
public class utilset {
    private static final String RSA_KEY_ALGORITHM = "RSA";
    public static final String ISSUER = "NyanId";
    public static final int SECRET_SIZE = 32;
    public static final String RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
    static int window_size = 1;
    static long second_per_size = 30L;

    private final StrictIpResolver strictIpResolver;

    public utilset(StrictIpResolver strictIpResolver) {
        this.strictIpResolver = strictIpResolver;
    }

    /**
     * UUID util
     *
     * @param uuiDtype //prefix
     * @param Char //char
     * @param Short //isShort?
     * @return UUID
     */
    public String GenerateUUID(UUIDtype uuiDtype , boolean Short , @NonNull String Char){
        byte[] bytes = (uuiDtype.getName() + Char).getBytes(StandardCharsets.UTF_8);
        if(Short){
            return String.valueOf(UUID.nameUUIDFromBytes(bytes)).replace("-", "");
        }
        return String.valueOf(UUID.nameUUIDFromBytes(bytes));
    }

    public String ShortUUIDtoFull(String shortuuid) {
        if (shortuuid.length() == 32){
            String fulluuid = UUID.fromString(shortuuid
                    .replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                            "$1-$2-$3-$4-$5")
            ).toString();
            return fulluuid;
        }else {
            return shortuuid;
        }
    }

    private String cleanKey(String key) {
        return key.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\\s", "");
    }

    public String decrypt(String data, String key) {
        try {
            String cleanedKey = cleanKey(key);
            byte[] k = Base64.getDecoder().decode(cleanedKey);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(k);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
            PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
            Cipher cipher = Cipher.getInstance(RSA_KEY_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedData = Base64.getDecoder().decode(data);
            byte[] decrypt = cipher.doFinal(encryptedData);
            return new String(decrypt);
        } catch (Exception e) {
            return null;
        }
    }

    public String encrypt(String data, String key) {
        try {
            String cleanedKey = cleanKey(key);
            byte[] k = Base64.getDecoder().decode(cleanedKey);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(k);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
            PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
            Cipher cipher = Cipher.getInstance(RSA_KEY_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encrypt = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypt);
        } catch (Exception e) {
            Logger.getLogger("NyanID").warning("Encrypt error: " + e);
            return "false";
        }
    }

    public byte[] rsaDecrypt(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    public String GetSessionUUID(HttpServletRequest request,String uid) {
        String sessionID = request.getSession().getId();
        String ip = strictIpResolver.getStrictClientIp(request);
        byte[] bytes = (Arrays.toString(Base64.getEncoder().encode(sessionID.getBytes())) +ip+uid).getBytes(StandardCharsets.UTF_8);
        return String.valueOf(UUID.nameUUIDFromBytes(bytes)).replace("-", "");
    }



    /**
     * RSA sign
     *
     * @param data   Data
     * @param priKey privateKey
     * @return sign
     */
    public String sign(byte[] data, String priKey) throws Exception {
        byte[] pkey = Base64.getDecoder().decode(cleanKey(priKey));
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(pkey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
        Signature signature = Signature.getInstance("SHA1withRSA");
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        signature.initSign(privateKey);
        signature.update(data);
        byte[] sign = signature.sign();
        return Base64.getEncoder().encodeToString(sign);
    }

    /**
     * RSA verify sign
     *
     * @param data     Data
     * @param sign     sign data
     * @param pubKey   PublicKey
     * @return boolean0
     */
    public boolean verify(byte[] data, byte[] sign, byte[] pubKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_KEY_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(pubKey);
        PublicKey publicKey = keyFactory.generatePublic(x509KeySpec);
        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sign);
    }

    /**
     * 生成一个SecretKey，外部绑定到用户
     *
     * @return SecretKey
     */
    public String generateSecretKey() {
        SecureRandom sr;
        try {
            sr = SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM);
            sr.setSeed(getSeed());
            byte[] buffer = sr.generateSeed(SECRET_SIZE);
            Base32 codec = new Base32();
            byte[] bEncodedKey = codec.encode(buffer);
            String ret = new String(bEncodedKey);
            return ret.replaceAll("=+$", "");// 移除末尾的等号
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 生成二维码所需的字符串，注：这个format不可修改，否则会导致身份验证器无法识别二维码
     *
     * @param user   绑定到的用户名
     * @param secret 对应的secretKey
     * @return 二维码字符串
     */
    public String getQRBarcode(String user, String secret) {
        user = ISSUER + ":" + user;
        String format = "otpauth://totp/%s?secret=%s";
        String ret = String.format(format, user, secret);
        ret += "&issuer=" + ISSUER;
        return ret;
    }

    /**
     * 验证用户提交的code是否匹配
     *
     * @param secret 用户绑定的secretKey
     * @param code   用户输入的code
     * @return 匹配成功与否
     */
    public static boolean checkCode(String secret, int code) {
        Base32 codec = new Base32();
        byte[] decodedKey = codec.decode(secret);
        long timeMsec = System.currentTimeMillis();
        long t = (timeMsec / 1000L) / second_per_size;
        for (int i = -window_size; i <= window_size; ++i) {
            int hash;
            try {
                hash = verifyCode(decodedKey, t + i);
            } catch (Exception e) {
                Logger.getLogger("NyanID").warning(e.getMessage());
                throw new RuntimeException(e.getMessage());
            }
            if (code == hash) {
                return true;
            }
        }
        return false;
    }

    private static int verifyCode(byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }
        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);
        int offset = hash[20 - 1] & 0xF;
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;
        return (int) truncatedHash;
    }

    private static byte[] getSeed() {
        String str = ISSUER + System.currentTimeMillis() + ISSUER;
        return str.getBytes(StandardCharsets.UTF_8);
    }



    /**
     * @param length 随机数长度
     */
    public String RandomNumber(int length) {
        String characters = "0123456789";
        StringBuilder flt = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            flt.append(characters.charAt(index));
        }
        return flt.toString();
    }
    public static boolean isDaysBefore(LocalDateTime targetDateTime, int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        LocalDateTime cutoffDateTime = cutoffDate.atStartOfDay();
        return !targetDateTime.isBefore(cutoffDateTime);
    }
    public static int RandomIntNumberW() {
        Random random = new Random();
        return random.nextInt(90) + 10;
    }

    /**
     * @param length 字符串长度
     */
    public  String RandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder flt = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            flt.append(characters.charAt(index));
        }
        return flt.toString();
    }

    /**
     * @param value value
     */
    public String HMACSHA256(String key, String value) {
        byte[] hash = null;
        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            hash = sha256_HMAC.doFinal(value.getBytes());
        }catch (Exception e) {
            log.error(e.getMessage());
        }
        return Base64.getEncoder().encodeToString(hash);
    }



    /**
     * 裁剪用户头像
     *
     * @param srcImagePath 读取图片路径
     * @param toImagePath  写入图片路径
     * @param widthRatio   宽度缩小比例
     * @param heightRatio  高度缩小比例
     */
    public static void reduceImageByRatio(InputStream srcImagePath, Path toImagePath, String uid , int widthRatio, int heightRatio) throws IOException {
        try{
            // 构造Image对象
            BufferedImage src = ImageIO.read(srcImagePath);
            ImageIO.write(src, "png", new File(toImagePath+uid +".png"));
        }catch(Exception e){
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(e.toString());
        }finally{
            if(srcImagePath != null){
                srcImagePath.close();
            }
        }
    }

    /**
     * 长高等比例缩小图片
     * @param srcImagePath 读取图片路径
     * @param toImagePath 写入图片路径
     * @param ratio 缩小比例
     * @throws IOException
     */
    public void reduceImageEqualProportion(String srcImagePath,String toImagePath,int ratio) throws IOException {
        FileOutputStream out = null;
        try{
            //读入文件
            File file = new File(srcImagePath);
            // 构造Image对象
            BufferedImage src = ImageIO.read(file);
            int width = src.getWidth();
            int height = src.getHeight();
            // 缩小边长
            BufferedImage tag = new BufferedImage(width / ratio, height / ratio, BufferedImage.TYPE_INT_RGB);
            // 绘制 缩小  后的图片
            tag.getGraphics().drawImage(src, 0, 0, width / ratio, height / ratio, null);
            out = new FileOutputStream(toImagePath);
            ImageIO.write(tag, "png", out);
        }catch(Exception e){
            log.error(e.getMessage());
        }finally{
            if(out != null){
                out.close();
            }
        }
    }


}





