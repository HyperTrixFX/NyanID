package moe.koseirin.nyanruaineo.Minecraft.config.cfg;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/** 踢出消息模板配置，存储在 {@code proxy.kick-message} 下。 */
@Data
public class KickMessageConfig {
    private boolean enabled;
    /**
     * 踢出屏幕模板：{@code &} 颜色代码，{@code n}/{@code |} 换行符，以及 {@code $playerName} / {@code $reason} / {@code $idRandom} 占位符。
     */
    @JSONField(name = "banned_message_base")
    private String bannedMessageBase;
}