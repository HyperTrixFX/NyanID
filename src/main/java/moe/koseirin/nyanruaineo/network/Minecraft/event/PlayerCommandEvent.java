package moe.koseirin.nyanruaineo.network.Minecraft.event;

import lombok.Getter;
import moe.koseirin.nyanruaineo.eventbus.Interface.Cancellable;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fired synchronously on the EventBus whenever a player sends a command (a chat message starting
 * with {@code /}) through the proxy. Listeners can {@link #setCancelled(boolean) cancel} the event
 * to consume the command so it is NOT forwarded to the backend server, and can answer the player
 * through {@link #reply(String)}.
 */
@Getter
public class PlayerCommandEvent implements Cancellable {

    /** The proxied player connection that issued the command. */
    private final UserConnection user;
    private final String username;
    private final UUID uuid;
    /** The command name including the leading slash, e.g. {@code /server}. */
    private final String command;
    /** The arguments after the command name (may be empty). */
    private final String args;

    /** Sends a chat message back to the issuing player. */
    private final Consumer<String> replier;

    private boolean cancelled;

    public PlayerCommandEvent(UserConnection user, String command, String args, Consumer<String> replier) {
        this.user = user;
        this.username = user.getUsername();
        this.uuid = user.getUuid();
        this.command = command;
        this.args = args;
        this.replier = replier;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * The full raw command line, e.g. {@code /server lobby}.
     */
    public String getFullCommand() {
        return args == null || args.isEmpty() ? command : command + " " + args;
    }

    /**
     * Sends a chat message back to the player who issued the command.
     */
    public void reply(String message) {
        if (replier != null) {
            replier.accept(message);
        }
    }
}
