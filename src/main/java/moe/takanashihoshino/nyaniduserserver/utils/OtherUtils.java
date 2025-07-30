package moe.takanashihoshino.nyaniduserserver.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Random;
import java.util.logging.Logger;

public class OtherUtils {

    /**
    * @param length 随机数长度
     */
    public static String RandomNumber(int length) {
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
    public static String RandomString(int length) {
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
    public static String HMACSHA256(String key, String value) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(value.getBytes());
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
    public static void reduceImageByRatio(InputStream srcImagePath, Path toImagePath, String uid , int widthRatio, int heightRatio) throws IOException{
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
            e.printStackTrace();
        }finally{
            if(out != null){
                out.close();
            }
        }
    }
}
