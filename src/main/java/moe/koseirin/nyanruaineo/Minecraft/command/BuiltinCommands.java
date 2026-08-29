package moe.koseirin.nyanruaineo.Minecraft.command;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;
import moe.koseirin.nyanruaineo.Minecraft.service.BackendServer;
import moe.koseirin.nyanruaineo.Minecraft.service.BackendServerManager;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerKickService;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerQueryService;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerTransferService;
import moe.koseirin.nyanruaineo.Minecraft.service.ServerStatusService;
import org.springframework.stereotype.Component;

/**
 * 使用 {@link CommandManager} 注册代理自带的命令 ({@code /ping}、{@code /server}、{@code /broadcast}、
 * {@code /kick}、{@code /list}、{@code /status})，类似 BungeeCord 插件注册命令的方式。
 * 额外命令也是通过同样方式添加 — 构建一个 {@link ProxyCommand} 然后用 {@code commandManager.registerCommand(...)}—这样就可以在不改桥接或数据包流程的情况下扩展命令集合。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BuiltinCommands {

    private final CommandManager commandManager;
    private final MinecraftProxy proxy;
    private final BackendServerManager backendServerManager;
    private final PlayerTransferService playerTransferService;
    private final PlayerKickService playerKickService;
    private final PlayerQueryService playerQueryService;
    private final ServerStatusService serverStatusService;

    @PostConstruct
    public void register() {
        commandManager.registerCommand(new ProxyCommand("ping") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                sender.sendMessage("§aPong!");
            }
        });

        commandManager.registerCommand(new ProxyCommand("awa") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                sender.sendMessage("§lawa!");
            }
        });

        commandManager.registerCommand(new ProxyCommand("ip") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                handleIp(sender, args);
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                String prefix = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
                List<String> matches = new ArrayList<>();
                for (PlayerQueryService.PlayerInfo player : playerQueryService.getOnlinePlayers()) {
                    if (player.username() != null && player.username().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        matches.add(player.username());
                    }
                }
                return matches;
            }
        });

        commandManager.registerCommand(new ProxyCommand("server") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                handleServer(sender, String.join(" ", args));
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                String prefix = args.length > 0 ? args[args.length - 1].toLowerCase(Locale.ROOT) : "";
                List<String> matches = new ArrayList<>();
                for (String name : backendServerManager.serverNames()) {
                    if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        matches.add(name);
                    }
                }
                return matches;
            }
        });

        commandManager.registerCommand(new ProxyCommand("broadcast", null, "bc") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                String message = String.join(" ", args);
                if (message.isEmpty()) {
                    sender.sendMessage("§cUsage: /broadcast <message>");
                    return;
                }
                int recipients = proxy.broadcast("§b§l[全服广播] §r" + message);
                sender.sendMessage("§aBroadcast delivered to " + recipients + " player(s).");
            }
        });

        commandManager.registerCommand(new ProxyCommand("kick") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                handleKick(sender, args);
            }
        });

        commandManager.registerCommand(new ProxyCommand("list") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                handleList(sender);
            }
        });

        commandManager.registerCommand(new ProxyCommand("status") {
            @Override
            public void execute(CommandSender sender, String[] args) {
                handleStatus(sender);
            }
        });
    }

    private UserConnection userOf(CommandSender sender) {
        return sender instanceof PlayerCommandSender player ? player.getUser() : null;
    }

    /** {@code /ip [player]}：目标玩家的 IP，如果未提供名字，则为发送者自己的 IP。*/
    private void handleIp(CommandSender sender, String[] args) {
        String targetName = args.length > 0 ? args[0].trim() : "";
        if (targetName.isEmpty()) {
            UserConnection self = userOf(sender);
            if (self == null || self.getIp() == null) {
                sender.sendMessage("§cCannot resolve your IP.");
                return;
            }
            sender.sendMessage("§eYour IP: §a" + self.getIp());
            return;
        }

        UserConnection target = playerQueryService.getUserConnection(targetName);
        if (target == null || target.getIp() == null) {
            sender.sendMessage("§cPlayer not online: " + targetName);
            return;
        }
        sender.sendMessage("§e" + target.getUsername() + " §7IP: §a" + target.getIp());

    }

    /** {@code /server [name]}：列出服务器，或者将玩家切换到指定的服务器。 */
    private void handleServer(CommandSender sender, String args) {
        if (args.isEmpty()) {
            List<String> names = backendServerManager.serverNames();
            if (names.isEmpty()) {
                sender.sendMessage("§cNo sub-servers configured!");
            } else {
                sender.sendMessage("§eServers: §a" + String.join(", ", names));
            }
            return;
        }

        BackendServer target = backendServerManager.findByName(args);
        if (target == null) {
            sender.sendMessage("§cUnknown server: " + args);
            return;
        }

        UserConnection user = userOf(sender);
        if (user == null) {
            return;
        }
        ServerConnection current = user.getServer();
        if (current != null && !current.isClosed()
                && target.getHost() != null && target.getHost().equalsIgnoreCase(current.getHost())
                && target.getPort() == current.getPort()) {
            sender.sendMessage("§eYou are already connected to " + target.getName() + "!");
            return;
        }

        playerTransferService.transferIfOnline(user, target, sender::sendMessage);
    }

    /** 使用配置的踢出屏幕执行 {@code /kick <玩家> [原因]}。 */
    private void handleKick(CommandSender sender, String[] args) {
        UserConnection user = userOf(sender);
        if (user == null) {
            return;
        }
        //TODO 只有配置的管理员可以踢出(未完成)。
        if (!user.getUuid().equals(UUID.fromString("96c6500f-4c9c-4fd2-86a5-e633862022be"))) {
            sender.sendMessage("§l§4权限不足喵~");
            return;
        }

        String joined = String.join(" ", args);
        String[] parts = joined.split(" ", 2);
        String targetName = parts.length > 0 ? parts[0].trim() : "";
        if (targetName.isEmpty()) {
            sender.sendMessage("§cUsage: /kick <player> [reason]");
            return;
        }
        UserConnection target = playerQueryService.getUserConnection(targetName);
        if (target == null) {
            sender.sendMessage("§cPlayer not online: " + targetName);
            return;
        }
        String reason = parts.length > 1 && !parts[1].isBlank() ? parts[1].trim() : "Kicked by an operator";
        playerKickService.kick(target, reason);
        sender.sendMessage("§aKicked " + target.getUsername() + ".");
    }

    /** {@code /list}: 每个在线玩家及其当前服务器。 */
    private void handleList(CommandSender sender) {
        List<PlayerQueryService.PlayerInfo> players = playerQueryService.getOnlinePlayers();
        if (players.isEmpty()) {
            sender.sendMessage("§cNo players online.");
            return;
        }
        StringBuilder reply = new StringBuilder("§6§lOnline players (" + players.size() + "):");
        for (PlayerQueryService.PlayerInfo player : players) {
            reply.append("\n§7- §f").append(player.username())
                    .append(" §8[").append(player.serverName() == null ? "?" : player.serverName()).append(']');
        }
        sender.sendMessage(reply.toString());
    }

    /**{@code /status}：每个配置的服务器及其在线状态和玩家数量。 */
    private void handleStatus(CommandSender sender) {
        serverStatusService.getStatusesAsync().thenAccept(statuses -> {
            if (statuses.isEmpty()) {
                sender.sendMessage("§cNo sub-servers configured!");
                return;
            }
            StringBuilder reply = new StringBuilder("§6§lServer status:");
            for (ServerStatusService.ServerStatus status : statuses) {
                reply.append("\n§7- §f").append(status.name())
                        .append(" §8(").append(status.host()).append(':').append(status.port()).append(") ")
                        .append(status.online() ? "§aonline" : "§coffline")
                        .append("§7 players: §f").append(status.playerCount());
            }
            sender.sendMessage(reply.toString());
        });
    }
}
