package moe.koseirin.nyanruaineo.websocket.Interceptor;

/*
 * @author KoseiRin_
 * awa
 */

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class BungeeAuthHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpHeaders headers = servletRequest.getHeaders();
            String sid = headers.getFirst("X-SID");
            String key = headers.getFirst("X-KEY");
            if (sid != null && key != null) {
                attributes.put("sid", sid);
                attributes.put("key", key);
                return true; // continue
            } else {
                // refuse
                return false;
            }
        }
        return false; // refuse
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response, @NonNull WebSocketHandler wsHandler, @Nullable Exception exception) {

    }
}
