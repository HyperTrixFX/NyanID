package moe.koseirin.nyanruaineo.utils.System.Command.CommandList;

import moe.koseirin.nyanruaineo.services.PermissionService;
import moe.koseirin.nyanruaineo.utils.System.Command.Command;
import moe.koseirin.nyanruaineo.utils.System.PermissionNodes;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

/*
 * @author KoseiRin_
 * awa
 */
@Component
public class OpCommand implements Command {

    private final PermissionService permissionService;

    public OpCommand(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public String getName() {
        return "/op";
    }

    @Override
    public String getDescription() {
        return "把一个用户升级为root用户,/op [uid]";
    }

    @Override
    public void execute(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        if (args.length > 0) {
            if (args[0] != null){
                //op <uid> —— 授予 uid 根权限（*）
                permissionService.grant(args[0], PermissionNodes.ROOT);
                Logger.getLogger("NyanID").info("已授予 uid [" + args[0] + "] root权限");
            }else Logger.getLogger("NyanID").info("请输入uid");

        }else Logger.getLogger("NyanID").warning("op 参数不足: [uid]");

    }
}
