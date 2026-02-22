package moe.koseirin.nyanruaineo.utils.Command.CommandList;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.Command.Command;

import java.util.Arrays;


public class HelloCommand implements Command {

    @Override
    public String getName() {
        return "hello";
    }

    @Override
    public String getDescription() {
        return "你好，世界";
    }

    @Override
    public void execute(String[] args) {
        System.out.println("Hello, World!" + Arrays.toString(args));
    }
}