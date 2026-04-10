package moe.koseirin.nyanruaineo.utils.System.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.System.Command.Command;
import moe.koseirin.nyanruaineo.utils.System.Command.CommandManager;

import java.util.logging.Logger;

public class HelpCommand implements Command {

    @Override
    public String getName() {
        return "/help";
    }

    @Override
    public String getDescription() {
        return "这个当然是显示帮助的呀,杂鱼喵~";
    }

    @Override
    public void execute(String[] args) {
            Logger.getLogger("NyanID").info("--这是一个帮助页面喵--");
            CommandManager.commands.values().forEach(key ->   Logger.getLogger("NyanID").info("已注册的指令: ["+ key.getName() + "] --" + key.getDescription()));
    }
}
