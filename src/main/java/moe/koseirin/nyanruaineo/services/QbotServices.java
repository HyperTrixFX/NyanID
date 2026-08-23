package moe.koseirin.nyanruaineo.services;

import com.alibaba.fastjson2.JSONObject;
import io.github.kloping.qqbot.Starter;
import io.github.kloping.qqbot.api.Intents;
import io.github.kloping.qqbot.api.v2.FriendMessageEvent;
import io.github.kloping.qqbot.api.v2.GroupMessageEvent;
import io.github.kloping.qqbot.impl.ListenerHost;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.eventbus.EventBus;
import moe.koseirin.nyanruaineo.eventbus.SysEvent.QbotFriendMessageReceivedEvent;
import moe.koseirin.nyanruaineo.eventbus.SysEvent.QbotGroupMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/*
 * @author KoseiRin_
 * awa
 */
@Service
@Slf4j
public class QbotServices {

    @Value("${NyanidSetting.Qbot.isEnable}")
    private boolean IsEnable;

    @Value("${NyanidSetting.Qbot.appid}")
    private String appid;

    @Value("${NyanidSetting.Qbot.secret}")
    private String secret;

    private final AIServices aiServices;
    private final EventBus eventBus;

    public QbotServices(AIServices aiServices, EventBus eventBus) {
        this.aiServices = aiServices;
        this.eventBus = eventBus;
    }

    @PostConstruct
    public void initQbot() {
        Starter starter = new Starter(appid, secret);
        starter.getConfig().setCode(Intents.PUBLIC_INTENTS.and(Intents.GROUP_INTENTS));
        starter.run();
        starter.registerListenerHost(new ListenerHost() {

            @EventReceiver
            public void onFriendMessage(FriendMessageEvent event) {
                eventBus.postAsync(new QbotFriendMessageReceivedEvent(event));
//                JSONObject msg = JSONObject.parseObject(event.getMetadata().toJSONString());
//                event.send(aiServices.chat(msg.getJSONObject("author").getString("union_openid"), msg.getString("content"),"null"));
//                log.warn(msg.getString("content"));
            }

            @EventReceiver
            public void onGroupMessage(GroupMessageEvent event) {
                eventBus.postAsync(new QbotGroupMessageReceivedEvent(event));
//                JSONObject msg = JSONObject.parseObject(event.getMetadata().toJSONString());
//                event.send(aiServices.chat(msg.getJSONObject("author").getString("union_openid"), msg.getString("content"),msg.getJSONObject("author").getString("username")));
//                log.warn(msg.getString("content"));
            }
        });
    }





}
