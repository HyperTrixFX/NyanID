package moe.koseirin.nyanruaineo.eventbus;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
public class EventBusAutoRegister implements ApplicationRunner {

    private final ApplicationContext applicationContext;

    private final EventBus eventBus;

    public EventBusAutoRegister(ApplicationContext applicationContext, EventBus eventBus) {
        this.applicationContext = applicationContext;
        this.eventBus = eventBus;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) throws Exception {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            if (hasSubscribeMethod(bean.getClass())) {
                eventBus.register(bean);
            }
        }
    }

    private boolean hasSubscribeMethod(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(EventHeader.class)) {
                return true;
            }
        }
        return false;
    }
}