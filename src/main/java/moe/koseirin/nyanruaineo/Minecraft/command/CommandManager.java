package moe.koseirin.nyanruaineo.Minecraft.command;

/*
 * @author KoseiRin_
 * awa
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * 代理的命令注册表，类似 BungeeCord 的 {@code PluginManager} 命令映射：命令按名称和别名注册，
 * 玩家输入的命令会分发给第一个匹配的命令，
 * 自动补全会返回到相应的命令处理。未注册的命令会返回 {@code false}/{@code null}，
 * 这样调用者就会原样转发给后端。
 */
@Component
public class CommandManager {

    private final Map<String, ProxyCommand> commands = new ConcurrentHashMap<>();

    /** 顾名思义。 */
    public void registerCommand(ProxyCommand command) {
        commands.put(command.getName().toLowerCase(Locale.ROOT), command);
        for (String alias : command.getAliases()) {
            commands.put(alias.toLowerCase(Locale.ROOT), command);
        }
    }

    /** 客户端命令树中公开的命令名称（包括别名），已排序。 */
    public List<String> getCommandNames() {
        List<String> names = new ArrayList<>(commands.keySet());
        names.sort(String::compareTo);
        return names;
    }

    /**
     * 如果命令已注册，则执行该命令。{@code commandLine} 是原始命令行，不包含前导斜杠（例如 {@code "server lobby"}）。
     * @return 当代理处理了命令（执行或权限被拒绝）时返回 true；当命令未知且应转发到后端时返回 false。
     */
    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        String[] split = commandLine.split(" ", -1);
        if (split.length == 0 || split[0].isEmpty()) {
            return false;
        }
        ProxyCommand command = commands.get(split[0].toLowerCase(Locale.ROOT));
        if (command == null) {
            return false;
        }
        if (!command.hasPermission(sender)) {
            sender.sendMessage("§c你没有权限使用这个指令");
            return true;
        }
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        command.execute(sender, args);
        return true;
    }

    /**
     * 代理命令的标签补全。{@code cursor} 是原始光标（例如 {@code "/server lob"}）。
     * @return 当光标不是代理命令时返回 null；否则返回（可能为空的）建议列表。
     */
    public List<String> tabComplete(CommandSender sender, String cursor) {
        String trimmed = cursor.startsWith("/") ? cursor.substring(1) : cursor;
        String[] split = trimmed.split(" ", -1);
        if (split.length == 0 || split[0].isEmpty()) {
            return null;
        }
        ProxyCommand command = commands.get(split[0].toLowerCase(Locale.ROOT));
        if (command == null) {
            return null;
        }
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        List<String> result = command.onTabComplete(sender, args);
        return result == null ? Collections.emptyList() : result;
    }
}
