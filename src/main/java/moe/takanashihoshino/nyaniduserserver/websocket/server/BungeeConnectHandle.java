package moe.takanashihoshino.nyaniduserserver.websocket.server;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import moe.takanashihoshino.nyaniduserserver.utils.RedisUtils.RedisService;
import moe.takanashihoshino.nyaniduserserver.entity.Accounts;
import moe.takanashihoshino.nyaniduserserver.repository.AccountsRepository;
import moe.takanashihoshino.nyaniduserserver.repository.ServerListRepository;
import moe.takanashihoshino.nyaniduserserver.websocket.Messages.UpdateOnlineSuccess;
import moe.takanashihoshino.nyaniduserserver.websocket.packet.S32;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ServerEndpoint("/api/zako/v3/websocket/bungee/{sid}/{key}")
public class BungeeConnectHandle implements ApplicationContextAware {

    private static ServerListRepository serverListRepository;
    private static RedisService redisService;
    private static AccountsRepository accountsRepository;
    private static AtomicInteger onlineCount = new AtomicInteger(0);
    private static CopyOnWriteArraySet<BungeeConnectHandle> webSocketSet = new CopyOnWriteArraySet<>();
    private Session session;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 静态字段通过应用上下文初始化
        serverListRepository = applicationContext.getBean(ServerListRepository.class);
        redisService = applicationContext.getBean(RedisService.class);
        accountsRepository = applicationContext.getBean(AccountsRepository.class);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid, @PathParam("key") String key) {
        this.session = session;
        // 确保静态字段已初始化
        if (serverListRepository == null) {
            log.error("serverListRepository 未初始化");
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART,  "10010");
            closeSession(session,closeReason);
            return;
        }
        String token = serverListRepository.findTokenByServerUid(sid);
        try {
            log.info("检测到BungeeCord连接请求，Sid: [{}]", sid);
            if (token != null){
                if (Objects.equals(key, token)){
                    webSocketSet.add(this);
                    sendHeartbeat();
                }else {
                    log.error("无效的会话Session: {}",session.getId());
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART,  "10001");
                    closeSession(session,closeReason);
                }
            }else {
                log.error("无效的会话Session: {}",session.getId());
                CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART,  "10001");
                closeSession(session,closeReason);
            }
        } catch (Exception e) {
            log.error("处理连接时异常", e);
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART,  "10001");
            closeSession(session,closeReason);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        if (isValidJsonStrict(message)){
        String packet = JSONObject.parseObject(message).getString("packet");
        String u = JSONObject.parseObject(message).getString("uuid");
        switch (packet){
            case "S29":
                sendHeartbeat();
                break;
            case "C30"://更新在线玩家数
                try {
                    String servername = JSONObject.parseObject(message).getString("servername");
                    int online = JSONObject.parseObject(message).getInteger("online");
                    redisService.setValueWithExpiration("Online-"+servername, online,30, TimeUnit.SECONDS);
                    UpdateOnlineSuccess response = new UpdateOnlineSuccess();
                    response.setS29(true);
                    session.getBasicRemote().sendText(JSONObject.toJSONString(response));
                }catch (Exception e){
                    log.error("处理更新在线玩家数时异常, {}", e.getMessage());
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR,  "10001");
                    session.close(closeReason);
                }
                break;
           case "C31"://bindMinecraftAccount
               try {
                   String code = JSONObject.parseObject(message).getString("code");
                   String uuid = JSONObject.parseObject(message).getString("uuid");
                   redisService.setValueWithExpiration(code, uuid,180, TimeUnit.SECONDS);
                   UpdateOnlineSuccess response = new UpdateOnlineSuccess();
                   response.setS29(true);
                   session.getBasicRemote().sendText(JSONObject.toJSONString(response));
               }catch (Exception e){
                   CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR,  "10001");
                   closeSession(session,closeReason);
                   log.error("处理绑定Minecraft账号时异常, {}", e.getMessage());
               }
                break;
            case "C32"://check bind
                try {
                    Accounts accounts = accountsRepository.GetUser(""+u);
                    S32 s32 = new S32();
                    if (accounts == null){
                        s32.setBind(false);
                    }else {
                        s32.setBind(true);
                        s32.setMuid(accounts.getBind());
                        s32.setUuid(accounts.getUid());
                        s32.setUsername(accounts.getUsername());
                    }
                    session.getBasicRemote().sendText(JSONObject.toJSONString(s32));
                }catch (Exception e){
                    CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.PROTOCOL_ERROR,  "10001");
                    closeSession(session,closeReason);
                    log.error("处理绑定Minecraft账号时异常, {}", e.getMessage());
                }
                break;


            default:
                log.error("未知的包类型: {}", packet);
                CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART,  "10001");
                closeSession(session,closeReason);
                break;
        }

    }else {
            log.error("未知的包类型");
            CloseReason closeReason = new CloseReason(CloseReason.CloseCodes.SERVICE_RESTART,  "10001");
            closeSession(session,closeReason);
        }


    }

    @OnClose
    public void onClose(Session session) {
        onlineCount.decrementAndGet();
        webSocketSet.remove(this);
        log.warn("Bungee连接断开，Session: {}", session.getId());
    }


    private void sendHeartbeat() {
        for (BungeeConnectHandle webSocket : webSocketSet) {
            try {
                webSocket.session.getBasicRemote().sendText("{\"packet\":\"C01\"}");
            } catch (IOException e) {
                log.error("发送心跳失败", e);
            }
        }
    }



    public static boolean isValidJsonStrict(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            Object parsed = JSON.parse(json);
            return parsed instanceof JSONObject || parsed instanceof JSONArray;
        } catch (JSONException e) {
            return false;
        }
    }
    private void closeSession(Session session,CloseReason closeReason) {
        try {
            if (session.isOpen()) {
                session.close(closeReason);
            }
        } catch (IOException e) {
            log.error("关闭会话异常", e);
        }
    }

    public static String sendMessage(String message) {
        for (BungeeConnectHandle webSocket : webSocketSet) {
            try {
                webSocket.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("发送消息失败", e);
            }
        }
        return message;
    }
}