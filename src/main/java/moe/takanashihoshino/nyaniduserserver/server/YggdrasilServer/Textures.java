package moe.takanashihoshino.nyaniduserserver.server.YggdrasilServer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import moe.takanashihoshino.nyaniduserserver.utils.ErrUtils.ErrRes;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.UserDevicesRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.YggdrasilPlayerRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Repository.YggdrasilRepository;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.Service.TexturesListService;
import moe.takanashihoshino.nyaniduserserver.utils.SqlUtils.TexturesList;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.zip.CRC32;

@RestController
@RequestMapping("api/yggdrasil/textures")
public class Textures {
    private final YggdrasilPlayerRepository yggdrasilPlayerRepository;
    private final YggdrasilRepository yggdrasilRepository;
    private final UserDevicesRepository userDevicesRepository;

    private final TexturesListService texturesListService;

    private static final byte[] PNG_HEADER = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D,
            0x49, 0x48, 0x44, 0x52
    };
    private static final byte[] PNG_CMIM = {
            (byte) 0x00, 0x00
    };
    private static final byte[] PNG_ColorType = {
            (byte) 0x06
    };

    public Textures(YggdrasilPlayerRepository yggdrasilPlayerRepository, YggdrasilRepository yggdrasilRepository, UserDevicesRepository userDevicesRepository, TexturesListService texturesListService) {
        this.yggdrasilPlayerRepository = yggdrasilPlayerRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.texturesListService = texturesListService;
    }

    @PutMapping("skin")
    public <T> CompletableFuture<Object> PutSkin(@RequestParam(value = "skin", required = false) T skin, @RequestParam(value = "model", required = false) T model, HttpServletResponse response, HttpServletRequest request) throws Exception {
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        if (yggdrasilRepository.GetPlayerNAME(uid) != null) {
            if (skin != null){
            if (isValidPng((MultipartFile) skin)){
                MultipartFile skinFile = (MultipartFile) skin;
                InputStream inputStream =  skinFile.getInputStream();
                String hash = getHash(inputStream);
                inputStream.close();
                Path SKINTexture = Paths.get("Data/YggdrasilTexture/hash-" + hash );
                File file = new File(SKINTexture.toString());
                int type;
                if (model != null){
                    type = switch ((String) model) {
                        case "default" -> 1;
                        case "slim" -> 0;
                        default -> 1;
                    };
                }else {
                    type = 1;
                }
                if (file.exists()){
                    String uuid = yggdrasilRepository.GetPlayerUUID(uid);
                    yggdrasilRepository.UpdateUseSkin(true,uid);
                    yggdrasilPlayerRepository.UpdateSkinTexturesHash(hash,uuid);
                    yggdrasilPlayerRepository.UpdateSkinTexturesType(type,uuid);
                    response.setStatus(204);
                    return CompletableFuture.completedFuture(null);
                }else {
                    Path p = Paths.get("Data/YggdrasilTexture/hash-");
                    String uuid = yggdrasilRepository.GetPlayerUUID(uid);
                    yggdrasilRepository.UpdateUseSkin(true,uid);
                    yggdrasilPlayerRepository.UpdateSkinTexturesHash(hash,uuid);
                    yggdrasilPlayerRepository.UpdateSkinTexturesType(type,uuid);
                    TexturesList texturesList = new TexturesList();
                    texturesList.setHash(hash);
                    texturesList.setModel(type);
                    texturesList.setType(true);
                    texturesList.setUid(uid);
                    texturesList.setCreate_time(System.currentTimeMillis());
                    texturesListService.save(texturesList);
                    try (InputStream inputStream1 = skinFile.getInputStream()) {
                        BufferedImage src = ImageIO.read(inputStream1);
                        ImageIO.write(src, "png", new File(p + hash));
                    } catch (Exception e) {
                        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(e.toString());
                    }
                    response.setStatus(204);
                    return CompletableFuture.completedFuture(null);
                }
        }else return CompletableFuture.completedFuture(ErrRes.IllegalRequestException("非法图像文件喵！", response));
        }else return CompletableFuture.completedFuture(ErrRes.IllegalRequestException("RequestParam skin is NULL  MiaoWu~", response));
    }else return CompletableFuture.completedFuture(ErrRes.IllegalRequestException("您不存在Yggdrasil账户", response));
}

    @PutMapping("cape")
    public <T> CompletableFuture<Object> PutCape(@RequestParam(value = "cape", required = false) T cape, HttpServletResponse response, HttpServletRequest request) throws Exception {
        String Authorization = request.getHeader("Authorization");
        String Token = Authorization.replace("Bearer ", "").replace(" ", "");
        String uid = userDevicesRepository.findUidByToken(Token);
        if (yggdrasilRepository.GetPlayerNAME(uid) != null) {
            if (cape != null){
                if (isValidPng((MultipartFile) cape)){
                    MultipartFile capeFile = (MultipartFile) cape;
                    InputStream inputStream =  capeFile.getInputStream();
                    String hash = getHash(inputStream);
                    inputStream.close();
                    Path SKINTexture = Paths.get("Data/YggdrasilTexture/hash-" + hash );
                    File file = new File(SKINTexture.toString());
                    if (file.exists()){
                        String uuid = yggdrasilRepository.GetPlayerUUID(uid);
                        yggdrasilRepository.UpdateUseCAPE(true,uid);
                        yggdrasilPlayerRepository.UpdateSkinCAPETexturesHash(hash,uuid);
                        response.setStatus(204);
                        return CompletableFuture.completedFuture(null);
                    }else {
                        Path p = Paths.get("Data/YggdrasilTexture/hash-");
                        String uuid = yggdrasilRepository.GetPlayerUUID(uid);
                        yggdrasilRepository.UpdateUseCAPE(true,uid);
                        yggdrasilPlayerRepository.UpdateSkinCAPETexturesHash(hash,uuid);
                        TexturesList texturesList = new TexturesList();
                        texturesList.setHash(hash);
                        texturesList.setType(false);
                        texturesList.setUid(uid);
                        texturesList.setCreate_time(System.currentTimeMillis());
                        texturesListService.save(texturesList);
                        try (InputStream inputStream1 = capeFile.getInputStream()) {
                            BufferedImage src = ImageIO.read(inputStream1);
                            ImageIO.write(src, "png", new File(p + hash));
                        } catch (Exception e) {
                            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(e.toString());
                        }
                        response.setStatus(204);
                        return CompletableFuture.completedFuture(null);
                    }
                }else return CompletableFuture.completedFuture(ErrRes.IllegalRequestException("非法图像文件喵！", response));
            }else return CompletableFuture.completedFuture(ErrRes.IllegalRequestException("RequestParam cape is NULL  MiaoWu~", response));
        }else return CompletableFuture.completedFuture(ErrRes.IllegalRequestException("您不存在Yggdrasil账户", response));
    }


    private static String getHash(InputStream fis) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] byteArray = new byte[1024];
        int bytesCount;
        while ((bytesCount = fis.read(byteArray)) != -1) {digest.update(byteArray, 0, bytesCount);}
        byte[] bytes = digest.digest();
        StringBuilder f = new StringBuilder();
        for (byte b : bytes) {
            f.append(String.format("%02x", b));
        }
        return f.toString();
    }


    public Boolean isValidPng(MultipartFile file) {
        try (InputStream fis = file.getInputStream()) {
            byte[] Infile = new byte[33];
            fis.read(Infile);
            fis.close();
            byte[] Header = {Infile[0], Infile[1], Infile[2], Infile[3], Infile[4], Infile[5], Infile[6], Infile[7], Infile[8], Infile[9], Infile[10], Infile[11], Infile[12], Infile[13], Infile[14], Infile[15]};
            if (Arrays.equals(Header, PNG_HEADER)) {
//                byte[] Width = {Infile[16], Infile[17], Infile[18], Infile[19]};
//                byte[] Height = {Infile[20], Infile[21], Infile[22], Infile[23]};
//                byte[] BitDepth = {Infile[24]};
                byte[] ColorType = {Infile[25]};
                byte[] CompressionMethodAndInterlaceMethod = {Infile[26], Infile[27]};
                byte[] CRC = {Infile[29], Infile[30], Infile[31], Infile[32]};
                if (Arrays.equals(ColorType, PNG_ColorType) && Arrays.equals(CompressionMethodAndInterlaceMethod, PNG_CMIM)) {
                    byte[] GetCal = {Infile[12], Infile[13], Infile[14], Infile[15], Infile[16], Infile[17], Infile[18], Infile[19], Infile[20], Infile[21], Infile[22], Infile[23], Infile[24], Infile[25], Infile[26], Infile[27], Infile[28]};
                    CRC32 crc32 = new CRC32();
                    crc32.update(GetCal);
                    return Long.toHexString(crc32.getValue()).toUpperCase().equals(bytesToHex(CRC).toUpperCase());
                } else return false;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            int unsignedByte = b & 0xFF;
            String hex = Integer.toHexString(unsignedByte).toUpperCase();
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }


}