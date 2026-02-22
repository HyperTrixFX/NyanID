package moe.koseirin.nyanruaineo.API;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.logging.Logger;

public interface Plugin {
    void onEnable();

    void onDisable();

    String PluginName();

    Logger getLogger();
}
