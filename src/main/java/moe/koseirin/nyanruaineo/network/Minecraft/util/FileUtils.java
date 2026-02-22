package moe.koseirin.nyanruaineo.network.Minecraft.util;

/*
 * @author KoseiRin_
 * awa
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtils {
    public static byte[] readFileToByteArray(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
    }
}