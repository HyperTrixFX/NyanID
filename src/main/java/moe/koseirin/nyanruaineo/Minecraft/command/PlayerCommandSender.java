package moe.koseirin.nyanruaineo.Minecraft.command;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;

/**
 * 在线代理玩家的 {@link CommandSender} 视图。回复通过共享的 {@code PlayerMessageService} 发送，
 * 因此它们会遵循代理的聊天格式。
 */
public class PlayerCommandSender implements CommandSender {

    @Getter
    private final UserConnection user;
    private final MinecraftProxy proxy;

    public PlayerCommandSender(UserConnection user, MinecraftProxy proxy) {
        this.user = user;
        this.proxy = proxy;
    }

    @Override
    public String getName() {
        return user.getUsername();
    }

    @Override
    public void sendMessage(String message) {
        proxy.getPlayerMessageService().sendMessage(user, message);
    }

    @Override
    public boolean hasPermission(String permission) {
        //TODO 指令权限检查未实现
        // No permission system yet — every player may run every registered command.
        return true;
    }

}
