package moe.koseirin.nyanruaineo.utils.System.Command;

/*
 * @author KoseiRin_
 * awa
 */

/*
 * @author KoseiRin_
 * awa
 */

public  interface Command {
    String getName();

    String getDescription();
    void execute(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException;
}
