package moe.takanashihoshino.nyaniduserserver;

import moe.takanashihoshino.nyaniduserserver.utils.Reload.Refreshable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

    @Refreshable
    @SpringBootApplication
    @EnableScheduling
    @EnableAsync
    @EnableJpaRepositories(basePackages = "moe.takanashihoshino.nyaniduserserver.utils.SqlUtils")
    @EnableRedisRepositories(basePackages = "moe.takanashihoshino.nyaniduserserver.utils.RedisUtils")
    public class NyanIdUserserverApplication {
    public static  Path configPath = Paths.get("config");
    public static  Path DataPath = Paths.get("Data");
    public static Path UserAvatar = Paths.get("Data/UserAvatar");
    public static Path GIFAvatar = Paths.get("Data/GIFAvatar");
    public static Path IllegalWords = Paths.get("Data/IllegalWords");
    public static Path UserDataPath = Paths.get("Data/UserData");
    public static Path YggdrasilTexture = Paths.get("Data/YggdrasilTexture");
    public static Path PluginsPath = Paths.get("plugins");
    private HashMap <Class<?>,Class> Clazz = new HashMap<>();

    public static void main(String[] args) throws Exception {
        Logger.getLogger("NyanID").info("[NyanID-UserServer] ["+ LocalDateTime.now() +"] ConfigPath: /config");
        if (!Files.exists(configPath) || !Files.exists(DataPath) || !Files.exists(UserAvatar) || !Files.exists(UserDataPath) || !Files.exists(PluginsPath)|| !Files.exists(IllegalWords)|| !Files.exists(GIFAvatar)|| !Files.exists(YggdrasilTexture)) {
            Files.createDirectories(configPath);
            Files.createDirectories(DataPath);
            Files.createDirectories(UserAvatar);
            Files.createDirectories(UserDataPath);
            Files.createDirectories(PluginsPath);
            Files.createDirectories(IllegalWords);
            Files.createDirectories(GIFAvatar);
            Files.createDirectories(YggdrasilTexture);
        }else {
            Resource resource = new ClassPathResource("application.cfg");
            Path targetPath = configPath.resolve(Objects.requireNonNull(resource.getFilename().replace("cfg","yml")));
            if (!Files.exists(targetPath)) {
                Files.copy(resource.getInputStream(), targetPath);
                Logger.getLogger("NyanID").info("[NyanID-UserServer] ["+ LocalDateTime.now() +"] : 配置文件已复制到config文件夹,请修改配置文件再运行喵~");
                Logger.getLogger("NyanID").info("[NyanID-UserServer] ["+ LocalDateTime.now() +"] : Code By TakanashiHoshino");
                Logger.getLogger("NyanID").info("[NyanID-UserServer] ["+ LocalDateTime.now() +"] : 爱来自ABYDOS喵~");
            } else if (Files.exists(targetPath)) {
                Path sensiwords = Paths.get("Data/IllegalWords/sensi_words.txt");
                File file = new File(sensiwords.toString());
                if (file.canRead() && file.exists()){
                    FileInputStream fileInputStream = new FileInputStream(file);
                    List<String> list=new ArrayList<String>();
                    list.add(new String(fileInputStream.readAllBytes()));
                    Logger.getLogger("NyanID").info("[NyanID-UserServer] ["+ LocalDateTime.now() +"] : 敏感词加载完毕");
                }else Logger.getLogger("NyanID").warning("[NyanID-UserServer] ["+ LocalDateTime.now() +"] : 未检测到敏感词库文件,所有涉及文本将设置为Bypass");
                SpringApplication.run(NyanIdUserserverApplication.class, args);
            }
        }
    }

    }

