package moe.koseirin.nyanruaineo.Minecraft.config.cfg;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Data;

import java.util.List;

/** TabList 拦截配置存储在 {@code proxy.tablist} 下。 */
@Data
public class TabListConfig {
    private boolean enabled;
    /** 头部行（支持旧版§代码，{@code %online%}/{@code %max%}/{@code %server%} 占位符）。 */
    private List<String> header;
    /** 页脚行（格式与页眉相同）。*/
    private List<String> footer;
    /** 在 TabList 中显示的每个玩家名字前都会添加此内容（1.8-1.18.2）。{@code %server%} = 玩家当前的子服务器 */
    private String prefix;
    /** 添加到在 TabList 中显示的每个玩家名字后面（1.8-1.18.2）。{@code %server%} = 玩家当前的子服务器。*/
    private String suffix;
}