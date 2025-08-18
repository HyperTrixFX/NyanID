package moe.koseirin.nyanruaineo.websocket;

/*
 * @author KoseiRin_
 * awa
 */

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
public class WebSocketConfig implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

//    @Bean
//    public ServerEndpointExporter serverEndpointExporter() throws InstantiationException, IllegalAccessException {
//        return new ServerEndpointExporter().getClass().newInstance();
//    }
}