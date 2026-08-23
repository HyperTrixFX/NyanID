package moe.koseirin.nyanruaineo.utils.System.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import moe.koseirin.nyanruaineo.utils.System.Command.Command;
import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.repository.UserPermissionsRepository;
import moe.koseirin.nyanruaineo.utils.SqlService.UserPermissionsService;
import moe.koseirin.nyanruaineo.entity.UserPermissions;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.logging.Logger;

@Component
@RestController
public class SystemctlCommand implements Command {

    private final RedisService redisService;
    private final UserPermissionsService userPermissionsService;
    private final UserPermissionsRepository userPermissionsRepository;

    public SystemctlCommand(RedisService redisService, UserPermissionsService userPermissionsService, UserPermissionsRepository userPermissionsRepository) {
        this.redisService = redisService;
        this.userPermissionsService = userPermissionsService;
        this.userPermissionsRepository = userPermissionsRepository;
    }

    @Override
    public String getName() {
        return "/systemctl";
    }

    @Override
    public String getDescription() {
        return "管理系统[/systemctl alert NotificationType NotificationData NotificationTypeName Time],[/systemctl adminpwd (rest/set [Your Action] [Your key] [uid])]";
    }

    @Override
    public void execute(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        if (args.length > 0){
            switch (args[0]) {
                case "alert":{
                    if (args.length == 5){
                        JSONObject data = new JSONObject();
                        data.put("NotificationType", args[1]);
                        data.put("NotificationData",args[2]);
                        data.put("NotificationTypeName", args[3]);
                        Logger.getLogger("NyanID").info("设置成功");
                        redisService.setValueWithExpiration("ServerInfo",data,Long.parseLong(args[4]),java.util.concurrent.TimeUnit.SECONDS);
                    }else {
                        Logger.getLogger("NyanID").info("设置全站通知失败");
                    }
                    break;
                }
                case "adminpwd":{
                    if (Objects.equals(args[1], "rest")){
                        userPermissionsRepository.deleteAll();
                        Logger.getLogger("NyanID").info("已删除全部管理员");
                    }else {
                        UserPermissions userPermissions = new UserPermissions();
                        userPermissions.setUid(args[4]);
//                        userPermissions.setAdminKey(args[3]);
//                        userPermissions.setAction(args[2]);
                        userPermissions.setUserGroup("⨉NEKO HACKER⨉");
                        userPermissionsService.save(userPermissions);
                        Logger.getLogger("NyanID").info("设置成功");
                    }
                    break;
                }


                default:
                    Logger.getLogger("NyanID").warning("未知systemctl命令参数");

            }
            }
    }
}
