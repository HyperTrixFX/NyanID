package moe.koseirin.nyanruaineo.Minecraft.forge;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.protocol.packet.PluginMessage;

/**
 * Forge 握手的跟踪日志，对应BungeeCord的 {@code ForgeLogger}。
 */
@Slf4j
public final class ForgeLogger {

    private ForgeLogger() {
    }

    public enum LogDirection {
        RECEIVED,
        SENDING
    }

    public static void logClient(LogDirection direction, String state, PluginMessage message) {
        if (log.isDebugEnabled()) {
            log.debug("CLIENT {} in {}: [{}] {} bytes", direction, state,
                    message.getTag(), message.getData() == null ? 0 : message.getData().length);
        }
    }

    public static void logServer(LogDirection direction, String state, PluginMessage message) {
        if (log.isDebugEnabled()) {
            log.debug("SERVER {} in {}: [{}] {} bytes", direction, state,
                    message.getTag(), message.getData() == null ? 0 : message.getData().length);
        }
    }
}
