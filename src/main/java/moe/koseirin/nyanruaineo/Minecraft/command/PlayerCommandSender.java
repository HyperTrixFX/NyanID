package moe.koseirin.nyanruaineo.Minecraft.command;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;

import java.util.UUID;

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

    /**
     * 权限检查委托给 Spring 的 {@code PermissionService}：先由玩家的 Minecraft UUID 解析出
     * NyanID 账号 UID，再按权限节点（含 {@code *} 与 {@code x.*} 通配）判断。
     */
    @Override
    public boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        UUID uuid = user.getUuid();
        if (uuid == null) {
            return false;
        }
        return proxy.getPermissionService().hasPermission(uuid, permission);
    }

}
