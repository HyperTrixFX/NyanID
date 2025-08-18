package moe.koseirin.nyanruaineo.utils.Command;

/*
 * @author KoseiRin_
 * awa
 */

public  interface Command {
    String getName();

    String getDescription();
    void execute(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException;
}
