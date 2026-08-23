package moe.koseirin.nyanruaineo.network.Minecraft.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerCommandEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerDisconnectEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerJoinEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.PlayerLoginEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.event.ProxyPingEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.service.BackendServer;
import moe.koseirin.nyanruaineo.network.Minecraft.service.BackendServerManager;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PlayerKickService;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PlayerQueryService;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PlayerTransferService;
import moe.koseirin.nyanruaineo.network.Minecraft.service.ServerStatusService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Proxy event listener. The project's {@code EventBusAutoRegister} registers every bean whose
 * methods carry {@link EventHeader} automatically, so additional listeners can be written the
 * same way.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyEventListener {

    private final BackendServerManager backendServerManager;
    private final PlayerTransferService playerTransferService;
    private final MinecraftProxy proxy;
    private final PlayerKickService playerKickService;
    private final PlayerQueryService playerQueryService;
    private final ServerStatusService serverStatusService;

    @EventHeader
    public void onPlayerLogin(PlayerLoginEvent event) {
//        log.info("[ProxyEvent] PlayerLogin: {} ({}) protocol={} from {} requesting {}",
//                event.username(), event.uuid(), event.protocolVersion(), event.ip(), event.requestedServer());
    }

    @EventHeader
    public void onPlayerJoin(PlayerJoinEvent event) {
        log.info("[ProxyEvent] PlayerJoin: {} ({}) protocol={} joined {}:{}",
                event.username(), event.uuid(), event.protocolVersion(), event.serverHost(), event.serverPort());

    }

    @EventHeader
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event.username() != null) log.info("[ProxyEvent] PlayerDisconnect: {} ({})", event.username(), event.uuid());

    }

    @EventHeader
    public void onProxyPing(ProxyPingEvent event) {
        log.debug("[ProxyEvent] ProxyPing: protocol={} from {}", event.protocolVersion(), event.ip());
    }

    /**
     * Unified proxy command handler: every proxy-side command ({@code /ping}, {@code /server}, ...)
     * is matched here. A command consumed here cancels the event so {@code UpstreamBridge} does not
     * forward it to the backend; anything unhandled simply falls through and reaches the backend.
     */
    @EventHeader
    public void onPlayerCommand(PlayerCommandEvent event) {
//        log.info("[ProxyEvent] PlayerCommand: {} issued '{}'", event.getUsername(), event.getFullCommand());
        String command = event.getCommand();
        if (command.equalsIgnoreCase("/ping")) {
            event.reply("§aPong!");
            event.setCancelled(true);
            return;
        }
        if (command.equalsIgnoreCase("/server")) {
            handleServerCommand(event);
            return;
        }
        if (command.equalsIgnoreCase("/broadcast") || command.equalsIgnoreCase("/bc")) {
            String message = event.getArgs();
            if (message.isEmpty()) {
                event.reply("§cUsage: /broadcast <message>");
            } else {
                int recipients = proxy.broadcast("§b§l[全服广播] §r" + message);
                event.reply("§aBroadcast delivered to " + recipients + " player(s).");
            }
            event.setCancelled(true);
            return;
        }
        if (command.equalsIgnoreCase("/kick")) {
            handleKickCommand(event);
            return;
        }
        if (command.equalsIgnoreCase("/list")) {
            handleListCommand(event);
            return;
        }
        if (command.equalsIgnoreCase("/status")) {
            handleStatusCommand(event);
        }
    }

    /** Handles {@code /kick <player> [reason]} with the configured kick screen. */
    private void handleKickCommand(PlayerCommandEvent event) {
        event.setCancelled(true);

        String[] parts = event.getArgs().split(" ", 2);
        String targetName = parts.length > 0 ? parts[0].trim() : "";
        if (targetName.isEmpty()) {
            event.reply("§cUsage: /kick <player> [reason]");
            return;
        }
        UserConnection target = playerQueryService.getUserConnection(targetName);
        if (target == null) {
            event.reply("§cPlayer not online: " + targetName);
            return;
        }
        String reason = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : "Kicked by an operator";
        playerKickService.kick(target, reason);
        event.reply("§aKicked " + target.getUsername() + ".");
    }

    /** Handles {@code /list}: every online player with their current server. */
    private void handleListCommand(PlayerCommandEvent event) {
        event.setCancelled(true);

        List<PlayerQueryService.PlayerInfo> players = playerQueryService.getOnlinePlayers();
        if (players.isEmpty()) {
            event.reply("§cNo players online.");
            return;
        }
        StringBuilder reply = new StringBuilder("§6§lOnline players (" + players.size() + "):");
        for (PlayerQueryService.PlayerInfo player : players) {
            reply.append("\n§7- §f").append(player.username())
                    .append(" §8[").append(player.serverName() == null ? "?" : player.serverName()).append(']');
        }
        event.reply(reply.toString());
    }

    /** Handles {@code /status}: every configured server with its online state and player count. */
    private void handleStatusCommand(PlayerCommandEvent event) {
        event.setCancelled(true);

        serverStatusService.getStatusesAsync().thenAccept(statuses -> {
            if (statuses.isEmpty()) {
                event.reply("§cNo sub-servers configured!");
                return;
            }
            StringBuilder reply = new StringBuilder("§6§lServer status:");
            for (ServerStatusService.ServerStatus status : statuses) {
                reply.append("\n§7- §f").append(status.name())
                        .append(" §8(").append(status.host()).append(':').append(status.port()).append(") ")
                        .append(status.online() ? "§aonline" : "§coffline")
                        .append("§7 players: §f").append(status.playerCount());
            }
            event.reply(reply.toString());
        });
    }

    /** Handles {@code /server [name]}: list servers, or switch the player to the named one. */
    private void handleServerCommand(PlayerCommandEvent event) {
        // /server is always consumed by the proxy, whatever the outcome.
        event.setCancelled(true);

        String args = event.getArgs();
        if (args.isEmpty()) {
            List<String> names = backendServerManager.serverNames();
            if (names.isEmpty()) {
                event.reply("§cNo sub-servers configured!");
            } else {
                event.reply("§eServers: §a" + String.join(", ", names));
            }
            return;
        }

        BackendServer target = backendServerManager.findByName(args);
        if (target == null) {
            event.reply("§cUnknown server: " + args);
            return;
        }

        UserConnection user = event.getUser();
        ServerConnection current = user.getServer();
        if (current != null && !current.isClosed()
                && target.getHost() != null && target.getHost().equalsIgnoreCase(current.getHost())
                && target.getPort() == current.getPort()) {
            event.reply("§eYou are already connected to " + target.getName() + "!");
            return;
        }

        // Check the target is online before transferring; an offline server replies with an error.
        playerTransferService.transferIfOnline(user, target, event::reply);
    }
}
