package moe.koseirin.nyanruaineo.server.YggdrasilServer;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.servlet.http.HttpServletRequest;
import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import moe.koseirin.nyanruaineo.repository.YggdrasilPlayerRepository;
import moe.koseirin.nyanruaineo.repository.YggdrasilRepository;
import moe.koseirin.nyanruaineo.utils.ErrorUtils.ErrorResponse;
import moe.koseirin.nyanruaineo.utils.Respond;
import moe.koseirin.nyanruaineo.utils.SqlService.TexturesListService;
import moe.koseirin.nyanruaineo.entity.TexturesList;
import moe.koseirin.nyanruaineo.utils.utilset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Value("${yggdrasil.privateKey}")
    private String privateKey;
    @Value("${yggdrasil.publicKey}")
    private String publicKey;

    private final YggdrasilPlayerRepository yggdrasilPlayerRepository;
    private final YggdrasilRepository yggdrasilRepository;
    private final UserDevicesRepository userDevicesRepository;
    private final utilset utilset;
    private final TexturesListService texturesListService;
    private final Respond respond;

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

    public Textures(YggdrasilPlayerRepository yggdrasilPlayerRepository, YggdrasilRepository yggdrasilRepository, UserDevicesRepository userDevicesRepository, utilset utilset, TexturesListService texturesListService, Respond respond) {
        this.yggdrasilPlayerRepository = yggdrasilPlayerRepository;
        this.yggdrasilRepository = yggdrasilRepository;
        this.userDevicesRepository = userDevicesRepository;
        this.utilset = utilset;
        this.texturesListService = texturesListService;
        this.respond = respond;
    }

    @PutMapping("skin")
    public <T> CompletableFuture<ResponseEntity<?>> PutSkin(@RequestParam(value = "skin", required = false) T skin, @RequestParam(value = "model", required = false) T model, HttpServletRequest request) throws Exception {
        String Authorization = request.getHeader("Authorization");
        String raw = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(raw, privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
        if (yggdrasilRepository.GetPlayerNAME(uid) == null) {
            return CompletableFuture.completedFuture(respond.respond(MediaType.APPLICATION_JSON, 400,new ErrorResponse("您不存在Yggdrasil账户", "Illegal Request", "Illegal Request")));
        }
        if (skin == null) {
            return CompletableFuture.completedFuture(respond.respond(MediaType.APPLICATION_JSON, 400,new ErrorResponse("RequestParam skin is NULL  MiaoWu~", "Illegal Request", "Illegal Request")));
        }
        MultipartFile skinFile = (MultipartFile) skin;
        if (!isValidPng(skinFile)) {
            return CompletableFuture.completedFuture(respond.respond(MediaType.APPLICATION_JSON, 400,new ErrorResponse("非法图像文件喵！", "Illegal Request", "Illegal Request")));
        }

        InputStream inputStream = skinFile.getInputStream();
        String hash = getHash(inputStream);
        inputStream.close();

        int type = 1;
        if (model != null) {
            type = switch ((String) model) {
                case "default" -> 1;
                case "slim" -> 0;
                default -> 1;
            };
        }

        Path skinPath = Paths.get("Data/YggdrasilTexture/hash-" + hash);
        File file = new File(skinPath.toString());
        String uuid = yggdrasilRepository.GetPlayerUUID(uid);
        yggdrasilRepository.UpdateUseSkin(true, uid);
        yggdrasilPlayerRepository.UpdateSkinTexturesHash(hash, uuid);
        yggdrasilPlayerRepository.UpdateSkinTexturesType(type, uuid);

        if (!file.exists()) {
            TexturesList texturesList = new TexturesList();
            texturesList.setHash(hash);
            texturesList.setModel(type);
            texturesList.setType(true);
            texturesList.setUid(uid);
            texturesList.setCreate_time(System.currentTimeMillis());
            texturesListService.save(texturesList);

            try (InputStream inputStream1 = skinFile.getInputStream()) {
                BufferedImage src = ImageIO.read(inputStream1);
                ImageIO.write(src, "png", new File(String.valueOf(skinPath)));
            } catch (Exception e) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(e.toString());
            }
        }

        return CompletableFuture.completedFuture(ResponseEntity.status(204).build());
    }

    @PutMapping("cape")
    public <T> CompletableFuture<ResponseEntity<?>> PutCape(@RequestParam(value = "cape", required = false) T cape, HttpServletRequest request) throws Exception {
        String Authorization = request.getHeader("Authorization");
        String raw = Authorization.replace("Bearer ", "").replace(" ", "");
        String Token = utilset.decrypt(raw, privateKey);
        String uid = userDevicesRepository.findUidByToken(Token);
        if (yggdrasilRepository.GetPlayerNAME(uid) == null) {
            return CompletableFuture.completedFuture(respond.respond(MediaType.APPLICATION_JSON, 400,new ErrorResponse("您不存在Yggdrasil账户", "Illegal Request", "Illegal Request")));
        }
        if (cape == null) {
            return CompletableFuture.completedFuture(respond.respond(MediaType.APPLICATION_JSON, 400,new ErrorResponse("RequestParam cape is NULL  MiaoWu~", "Illegal Request", "Illegal Request")));
        }
        MultipartFile capeFile = (MultipartFile) cape;
        if (!isValidPng(capeFile)) {
            return CompletableFuture.completedFuture(respond.respond(MediaType.APPLICATION_JSON, 400,new ErrorResponse("非法图像文件喵！", "Illegal Request", "Illegal Request")));
        }

        InputStream inputStream = capeFile.getInputStream();
        String hash = getHash(inputStream);
        inputStream.close();

        Path capePath = Paths.get("Data/YggdrasilTexture/hash-" + hash);
        File file = new File(capePath.toString());
        String uuid = yggdrasilRepository.GetPlayerUUID(uid);
        yggdrasilRepository.UpdateUseCAPE(true, uid);
        yggdrasilPlayerRepository.UpdateSkinCAPETexturesHash(hash, uuid);

        if (!file.exists()) {
            TexturesList texturesList = new TexturesList();
            texturesList.setHash(hash);
            texturesList.setType(false);
            texturesList.setUid(uid);
            texturesList.setCreate_time(System.currentTimeMillis());
            texturesListService.save(texturesList);

            try (InputStream inputStream1 = capeFile.getInputStream()) {
                BufferedImage src = ImageIO.read(inputStream1);
                ImageIO.write(src, "png", new File(String.valueOf(capePath)));
            } catch (Exception e) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).warning(e.toString());
            }
        }

        return CompletableFuture.completedFuture(ResponseEntity.status(204).build());
    }

    private static String getHash(InputStream fis) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] byteArray = new byte[1024];
        int bytesCount;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }
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
            byte[] Header = {Infile[0], Infile[1], Infile[2], Infile[3], Infile[4], Infile[5], Infile[6], Infile[7],
                    Infile[8], Infile[9], Infile[10], Infile[11], Infile[12], Infile[13], Infile[14], Infile[15]};
            if (Arrays.equals(Header, PNG_HEADER)) {
                byte[] ColorType = {Infile[25]};
                byte[] CompressionMethodAndInterlaceMethod = {Infile[26], Infile[27]};
                byte[] CRC = {Infile[29], Infile[30], Infile[31], Infile[32]};
                if (Arrays.equals(ColorType, PNG_ColorType) && Arrays.equals(CompressionMethodAndInterlaceMethod, PNG_CMIM)) {
                    byte[] GetCal = {Infile[12], Infile[13], Infile[14], Infile[15], Infile[16], Infile[17], Infile[18], Infile[19],
                            Infile[20], Infile[21], Infile[22], Infile[23], Infile[24], Infile[25], Infile[26], Infile[27], Infile[28]};
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