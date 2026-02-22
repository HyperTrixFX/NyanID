package moe.koseirin.nyanruaineo.websocket;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.websocket.Handler.BungeeWebSocketHandler;
import moe.koseirin.nyanruaineo.websocket.Interceptor.BungeeAuthHandshakeInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BungeeWebSocketHandler bungeeWebSocketHandler;

    public WebSocketConfig(BungeeWebSocketHandler bungeeWebSocketHandler) {
        this.bungeeWebSocketHandler = bungeeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(bungeeWebSocketHandler, "/api/zako/v3/websocket/bungee")
                .addInterceptors(new BungeeAuthHandshakeInterceptor())
                .setAllowedOrigins("*");
    }


}