package moe.koseirin.nyanruaineo.eventbus.listener;

/*
 * @author KoseiRin_
 * awa
 */

import com.alibaba.fastjson2.JSONObject;
import io.github.kloping.qqbot.api.v2.FriendMessageEvent;
import io.github.kloping.qqbot.api.v2.GroupMessageEvent;
import io.github.kloping.qqbot.entities.ex.Keyboard;
import io.github.kloping.qqbot.entities.ex.Markdown;
import io.github.kloping.qqbot.entities.ex.MessageAsyncBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.entity.NyanIDuser;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.eventbus.SysEvent.QbotFriendMessageReceivedEvent;
import moe.koseirin.nyanruaineo.eventbus.SysEvent.QbotGroupMessageReceivedEvent;
import moe.koseirin.nyanruaineo.network.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.network.Minecraft.service.PlayerQueryService;
import moe.koseirin.nyanruaineo.network.Minecraft.service.ServerStatusService;
import moe.koseirin.nyanruaineo.services.AIServices;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class QbotListener {

    private final AIServices aiServices;
    private final MinecraftProxy proxy;
    private final PlayerQueryService playerQueryService;
    private final ServerStatusService serverStatusService;

    @EventHeader
    private void FriendMessageEventListener(QbotFriendMessageReceivedEvent event) {
        FriendMessageEvent friendMessageEvent = event.event();
        AtomicReference<JSONObject> data = new AtomicReference<>(JSONObject.parseObject(friendMessageEvent.getMetadata().toJSONString()));
        String union_openid = data.get().getJSONObject("author").getString("union_openid");
        String msg = data.get().getString("content");
        String[] parts = msg.trim().split(" ", 2);   // 按第一个空格拆分
        String command = parts[0];                   // 命令，如 /bc
        String args = parts.length > 1 ? parts[1] : ""; // 参数，如 message
        switch (command) {
            case "/help" -> {
                MessageAsyncBuilder builder = new MessageAsyncBuilder();
                Markdown markdown = Markdown.ofText("### 测试markdown大标题");
                builder.append(markdown);

                Keyboard.KeyboardBuilder keyboardBuilder = new Keyboard.KeyboardBuilder();
                Keyboard keyboard =
                        keyboardBuilder
                                .addRow()
                                .addButton()
                                .setLabel("测试标题")
                                .setVisitedLabel("测试标题.")
                                .setStyle(1)
                                .setActionData("123")
                                .setActionEnter(false)
                                .setActionReply(true)
                                .setActionType(2)
                                .build()
                                .build()
                                .build();
                builder.append(keyboard);
                friendMessageEvent.send(builder.build());
            }
            case "/status" -> {
                serverStatusService.getStatusesAsync().thenAccept(statuses -> {
                    if (statuses.isEmpty()) {
                        friendMessageEvent.send("No sub-servers configured!");
                        return;
                    }
                    StringBuilder reply = new StringBuilder("Server status:");
                    for (ServerStatusService.ServerStatus status : statuses) {
                        reply.append("\n- ").append(status.name())
                                .append(" (").append(status.host()).append(':').append(status.port()).append(") ")
                                .append(status.online() ? "online" : "offline")
                                .append(" players: ").append(status.playerCount());
                    }
                    friendMessageEvent.send(reply.toString());
                });

            }
            case "/list" ->{
                List<PlayerQueryService.PlayerInfo> players = playerQueryService.getOnlinePlayers();
                if (players.isEmpty()) {
                    friendMessageEvent.send("No players online.");
                    return;
                }
                StringBuilder reply = new StringBuilder("Online players (" + players.size() + "):");
                for (PlayerQueryService.PlayerInfo player : players) {
                    reply.append("\n- ").append(player.username())
                            .append(" [").append(player.serverName() == null ? "?" : player.serverName()).append(']');
                }
                friendMessageEvent.send(reply.toString());
            }
            case "/nyanid" -> friendMessageEvent.send("awa");
            case "/bc" -> friendMessageEvent.send("Broadcast delivered to " + proxy.broadcast("§b§l[全服广播] §r" + args) + " player(s).");
            case "d" -> friendMessageEvent.send("1");
            default -> friendMessageEvent.send(aiServices.chat(union_openid, msg, "null"));
        }
    }





    @EventHeader
    private void GroupMessageEventListener(QbotGroupMessageReceivedEvent event) {
        GroupMessageEvent groupMessageEvent = event.event();
        AtomicReference<JSONObject> data = new AtomicReference<>(JSONObject.parseObject(groupMessageEvent.getMetadata().toJSONString()));
        String union_openid = data.get().getJSONObject("author").getString("union_openid");
        String msg = data.get().getString("content");
        String[] parts = msg.trim().split(" ", 2);   // 按第一个空格拆分
        String command = parts[0];                   // 命令，如 /bc
        String args = parts.length > 1 ? parts[1] : ""; // 参数，如 message
        switch (command) {
            case "/help" -> {
                MessageAsyncBuilder builder = new MessageAsyncBuilder();
                Markdown markdown = Markdown.ofText("### 测试markdown大标题");
                builder.append(markdown);

                Keyboard.KeyboardBuilder keyboardBuilder = new Keyboard.KeyboardBuilder();
                Keyboard keyboard =
                        keyboardBuilder
                                .addRow()
                                .addButton()
                                .setLabel("测试标题")
                                .setVisitedLabel("测试标题.")
                                .setStyle(1)
                                .setActionData("123")
                                .setActionEnter(false)
                                .setActionReply(true)
                                .setActionType(2)
                                .build()
                                .build()
                                .build();
                builder.append(keyboard);
                groupMessageEvent.send(builder.build());
            }
            case "/status" -> {
                serverStatusService.getStatusesAsync().thenAccept(statuses -> {
                    if (statuses.isEmpty()) {
                        groupMessageEvent.send("No sub-servers configured!");
                        return;
                    }
                    StringBuilder reply = new StringBuilder("Server status:");
                    for (ServerStatusService.ServerStatus status : statuses) {
                        reply.append("\n- ").append(status.name())
                                .append(" (").append(status.host()).append(':').append(status.port()).append(") ")
                                .append(status.online() ? "online" : "offline")
                                .append(" players: ").append(status.playerCount());
                    }
                    groupMessageEvent.send(reply.toString());
                });

            }
            case "/list" ->{
                List<PlayerQueryService.PlayerInfo> players = playerQueryService.getOnlinePlayers();
                if (players.isEmpty()) {
                    groupMessageEvent.send("No players online.");
                    return;
                }
                StringBuilder reply = new StringBuilder("Online players (" + players.size() + "):");
                for (PlayerQueryService.PlayerInfo player : players) {
                    reply.append("\n- ").append(player.username())
                            .append(" [").append(player.serverName() == null ? "?" : player.serverName()).append(']');
                }
                groupMessageEvent.send(reply.toString());
            }
            case "/nyanid" -> groupMessageEvent.send("awa");
            case "/bc" -> groupMessageEvent.send("Broadcast delivered to " + proxy.broadcast("§b§l[全服广播] §r" + args) + " player(s).");
            case "d" -> groupMessageEvent.send("1");
            default -> groupMessageEvent.send(aiServices.chat(union_openid, msg, "null"));
        }


    }



    private NyanIDuser getUser(String union_openid) {


        return null;
    }
}
