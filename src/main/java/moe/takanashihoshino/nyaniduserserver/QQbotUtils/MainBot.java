package moe.takanashihoshino.nyaniduserserver.QQbotUtils;


import jakarta.annotation.PostConstruct;
import moe.takanashihoshino.nyaniduserserver.QQbotUtils.impl.onMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MainBot {

    @Value("${NyanidSetting.qqbot.appid}")
    private String appid;

    @Value("${NyanidSetting.qqbot.token}")
    private String token;

    @Value("${NyanidSetting.qqbot.secret}")
    private String secret;

    @Value("${NyanidSetting.qqbot.isSandbox}")
    private Boolean isSandbox;

    @Value("${NyanidSetting.qqbot.isEnable}")
    private Boolean isEnable;

    @Autowired
    private onMessage onMessage;

    @Async
    @PostConstruct
    public void runbot() {
//        if (isEnable) {
//            Starter starter = new Starter(appid, token, secret);
//            if (isSandbox) {
//                starter.getConfig().setSandbox(true);
//            }
//            starter.APPLICATION.logger.setOutFile(null);
//            starter.getConfig().setCode(Intents.PUBLIC_INTENTS.and(Intents.GROUP_INTENTS));
//            starter.run();
//            starter.setReconnect(true);
//            starter.registerListenerHost(onMessage);
//        }
    }
}
