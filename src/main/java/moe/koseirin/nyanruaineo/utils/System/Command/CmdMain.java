package moe.koseirin.nyanruaineo.utils.System.Command;


/*
 * @author KoseiRin_
 * awa
 */


/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;
import moe.koseirin.nyanruaineo.utils.System.Command.CommandList.*;
import moe.koseirin.nyanruaineo.utils.System.Command.CommandList.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestController;

import java.util.logging.Logger;

@Component
@RestController
public class CmdMain {


    private final RedisCommand redisCommand;
    private final UserManagerCommand userManagerCommand;
    private final SystemctlCommand systemctlCommand;
    private final ReloadConfig reloadConfig;
    private final HelloCommand helloCommand;
    private final OpCommand opCommand;




    @Value("${NyanidSetting.EnableCommand}")
    private boolean EnableCommand;

    public CmdMain(RedisCommand redisCommand, UserManagerCommand userManagerCommand, SystemctlCommand systemctlCommand, ReloadConfig reloadConfig, HelloCommand helloCommand, OpCommand opCommand) {
        this.redisCommand = redisCommand;
        this.userManagerCommand = userManagerCommand;
        this.systemctlCommand = systemctlCommand;

        this.reloadConfig = reloadConfig;
        this.helloCommand = helloCommand;
        this.opCommand = opCommand;
    }

    @PostConstruct
    public void run() {
        if (EnableCommand){
        try {
            CommandManager commandManager = new CommandManager();
            //Register Commands
            commandManager.registerCommand(helloCommand);
            commandManager.registerCommand(new HelpCommand());
            commandManager.registerCommand(new StopCommand());
            commandManager.registerCommand(systemctlCommand);
            commandManager.registerCommand(redisCommand);
            commandManager.registerCommand(userManagerCommand);
            commandManager.registerCommand(reloadConfig);
            commandManager.registerCommand(opCommand);
            //END Register
            Thread consoleThread = new Thread(new ConsoleInputHandler(commandManager));
            consoleThread.start();
            Logger.getLogger("NyanID").info("NyanID-UserServer加载完成,您可以在控制台输入/help命令查看指令喵~");
        }catch (Exception e){
            Logger.getLogger("NyanID").warning("Console Thread Error :"+e);
        }
    }else {
            Logger.getLogger("NyanID").warning("NyanID-UserServer加载完成,控制台指令已在配置文件中禁用,你可以手动修改application.yml中的相关选项以启用喵~");
        }
    }
}
