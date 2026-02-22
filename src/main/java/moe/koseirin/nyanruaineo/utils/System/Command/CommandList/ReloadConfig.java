package moe.koseirin.nyanruaineo.utils.System.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.utils.System.Command.Command;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class ReloadConfig implements Command {

    private final SystemConfigCacheService systemConfigCacheService;

    public ReloadConfig(SystemConfigCacheService systemConfigCacheService) {
        this.systemConfigCacheService = systemConfigCacheService;
    }


    @Override
    public String getName() {
        return "rc";
    }

    @Override
    public String getDescription() {
        return "刷新配置文件";
    }

    @Override
    public void execute(String[] args) {
        systemConfigCacheService.loadConfigs();
        log.info("配置文件已刷新喵~");

    }
}
