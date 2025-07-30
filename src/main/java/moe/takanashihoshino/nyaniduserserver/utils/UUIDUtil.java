package moe.takanashihoshino.nyaniduserserver.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UUIDUtil {
    public static String generateUUID(String name, String NyanID){
        return String.valueOf(UUID.nameUUIDFromBytes(("NyanCatPlayer:" + name+NyanID).getBytes(StandardCharsets.UTF_8)));
    }

    public static String generateNyanIDUUID(String value, String email){
        return String.valueOf(UUID.nameUUIDFromBytes(("NyanIDUser:" + value+email.replaceAll("@", "").replaceAll("\\.", "")).getBytes(StandardCharsets.UTF_8)));
    }

    public static String generateUUIDbutIsOfflineMethod(String name){
        return String.valueOf(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
    }

    public static String generatesessionid(String sid){
        return String.valueOf(UUID.nameUUIDFromBytes(("BungeeServer:" + sid).getBytes(StandardCharsets.UTF_8)));
    }

    public static String get(String sid){
        return String.valueOf(UUID.nameUUIDFromBytes(("O4F1569" + sid).getBytes(StandardCharsets.UTF_8)));
    }


}
