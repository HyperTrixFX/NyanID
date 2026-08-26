package moe.koseirin.nyanruaineo.Minecraft.util;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RGBColorConverter {
    private static final Pattern GRADIENT_PATTERN = Pattern.compile(
            "<gradient:#([A-Fa-f0-9]{6}):#([A-Fa-f0-9]{6})>(.*?)</gradient>"
    );
    private static final Pattern RAINBOW_PATTERN = Pattern.compile(
            "<rainbow>(.*?)</rainbow>"
    );
    private static final Pattern HEX_PATTERN = Pattern.compile(
            "&#([A-Fa-f0-9]{6})"
    );

    /**
     * 将高级颜色格式转换为Minecraft可用的格式
     * @param text 原始文本（可能包含<gradient>、<rainbow>等标签）
     * @param clientVersion 客户端版本
     * @return 转换后的文本
     */
    public static String convert(String text, int clientVersion) {
        if (clientVersion >= 754) { // 1.16+
            return convertToHexFormat(text);
        } else {
            return convertToLegacy(text);
        }
    }

    private static String convertToHexFormat(String text) {
        // 处理渐变
        Matcher gradientMatcher = GRADIENT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (gradientMatcher.find()) {
            String startColor = gradientMatcher.group(1);
            String endColor = gradientMatcher.group(2);
            String content = gradientMatcher.group(3);
            gradientMatcher.appendReplacement(sb, content);
        }
        gradientMatcher.appendTail(sb);
        text = sb.toString();

        Matcher rainbowMatcher = RAINBOW_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (rainbowMatcher.find()) {
            String content = rainbowMatcher.group(1);
            rainbowMatcher.appendReplacement(sb, content);
        }
        rainbowMatcher.appendTail(sb);
        text = sb.toString();

        Matcher hexMatcher = HEX_PATTERN.matcher(text);
        sb = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            hexMatcher.appendReplacement(sb, replacement.toString());
        }
        hexMatcher.appendTail(sb);

        return sb.toString();
    }

    private static String convertToLegacy(String text) {
        text = GRADIENT_PATTERN.matcher(text).replaceAll("$3");
        text = RAINBOW_PATTERN.matcher(text).replaceAll("$1");
        text = HEX_PATTERN.matcher(text).replaceAll(""); // 移除HEX颜色

        return text;
    }
}
