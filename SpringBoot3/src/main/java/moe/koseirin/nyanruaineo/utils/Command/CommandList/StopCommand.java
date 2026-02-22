package moe.koseirin.nyanruaineo.utils.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.Command.Command;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class StopCommand implements Command {

    @Override
    public String getName() {
        return "/stop";
    }

    @Override
    public String getDescription() {
        return "终止SpringBoot";
    }

    @Override
    public void execute(String[] args) throws InterruptedException {
        Logger.getLogger("NyanID").warning("SpringBoot将在5s后关闭喵！！！");
        Thread.sleep(5000);
        System.exit(0);
    }
}
