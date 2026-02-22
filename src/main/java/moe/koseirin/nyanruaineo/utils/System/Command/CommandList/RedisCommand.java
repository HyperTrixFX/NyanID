package moe.koseirin.nyanruaineo.utils.System.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.RedisUtils.RedisService;
import moe.koseirin.nyanruaineo.utils.System.Command.Command;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@Component
public class RedisCommand implements Command {


    private final RedisService redisService;

    public RedisCommand(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public String getName() {
        return "/redis";
    }

    @Override
    public String getDescription() {
        return "使用控制台指令操作Redis";
    }

    @Override
    public void execute(String[] args) {
        if (args.length > 0){
            switch (args[0]){
                case "set":
                    if (args.length > 2){
                    redisService.setValue(args[1],args[2]);
                    Logger.getLogger("NyanID").info("设置成功");
                    break;
                    }else {
                        Logger.getLogger("NyanID").warning("参数错误,请输入有效的Key/value");
                        break;
                    }
                case "get":
                    if (args.length > 1){
                        Logger.getLogger("NyanID").info("Key为[" +args[1]+ "]的Value为 : "+redisService.getValue(args[1]));
                        break;
                    }else {
                        Logger.getLogger("NyanID").warning("参数错误,请输入有效的Key");
                        break;
                    }
                case "setTimeValue":
                    if (args.length > 3){
                        redisService.setValueWithExpiration(args[1],args[2],Long.parseLong(args[3]),java.util.concurrent.TimeUnit.SECONDS);
                        Logger.getLogger("NyanID").info("设置成功");
                        break;
                    }else {
                        Logger.getLogger("NyanID").warning("参数错误,请输入有效的Key/value/time(s)");
                        break;
                    }
                case "remove":
                    if (args.length > 1){
                        redisService.deleteValue(args[1]);
                        Logger.getLogger("NyanID").info("已删除Key为"+args[1]+"的Value");
                        break;
                    }else {
                        Logger.getLogger("NyanID").warning("参数错误,请输入有效的Key");
                        break;
                    }

                    default:
                        Logger.getLogger("NyanID").warning("参数错误,[get/set/setTimeValue/remove]");
                        break;
            }

        }else Logger.getLogger("NyanID").warning("参数错误,格式为/redis [get/set/setTimeValue/remove] [key] *value*");
    }
}
