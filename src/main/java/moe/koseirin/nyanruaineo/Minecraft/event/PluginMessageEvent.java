package moe.koseirin.nyanruaineo.Minecraft.event;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.Getter;
import moe.koseirin.nyanruaineo.eventbus.Interface.Cancellable;
import moe.koseirin.nyanruaineo.Minecraft.connection.ServerConnection;
import moe.koseirin.nyanruaineo.Minecraft.connection.UserConnection;

/**
 * 对于经过代理的每一条插件消息，都会在 EventBus 上同步触发此事件，与 BungeeCord 的
 * {@code PluginMessageEvent} 相对应。发送方通过 {@link #isFromServer()} 来标识：
 * 当 {@code true} 时表示后端向客户端发送消息，当 {@code false} 时表示客户端向后端发送消息。
 * 监听器可以取消此事件来吞没该消息（此时消息既不会被转发，也不会被 BungeeCord 的内置频道处理）。
 */
@Getter
public class PluginMessageEvent implements Cancellable {

    /** The proxied player the message is associated with. */
    private final UserConnection user;
    /** The backend connection involved (may be {@code null} while still connecting). */
    private final ServerConnection server;
    /** True when the message travelled server → client, false for client → server. */
    private final boolean fromServer;
    /** The channel name (already modernised for 1.13+). */
    private final String tag;
    /** A copy of the payload. */
    private final byte[] data;

    private boolean cancelled;

    public PluginMessageEvent(UserConnection user, ServerConnection server, String tag,
                              byte[] data, boolean fromServer) {
        this.user = user;
        this.server = server;
        this.tag = tag;
        this.data = data.clone();
        this.fromServer = fromServer;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
