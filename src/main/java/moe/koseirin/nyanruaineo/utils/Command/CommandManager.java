package moe.koseirin.nyanruaineo.utils.Command;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class CommandManager {
    public static Map<String, Command> commands = new HashMap<>();

    public void registerCommand(Command command ) {
        commands.put(command.getName(), command);
    }

    public void executeCommand(String commandName, String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        Command command = commands.get(commandName);
        if (command != null) {
            try {
                command.execute(args);
            }catch (Exception e){
                Logger.getLogger("NyanID").warning("[COMMAND]服务器内部错误 :"+e);
            }

        } else {
            Logger.getLogger("NyanID").warning("Unknown command: [ " + commandName +" ],Please use /help for help Ciallo~");
        }
    }
}
