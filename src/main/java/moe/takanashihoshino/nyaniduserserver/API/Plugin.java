package moe.takanashihoshino.nyaniduserserver.API;

import java.util.logging.Logger;

public interface Plugin {
    void onEnable();

    void onDisable();

    String PluginName();

    Logger getLogger();
}
