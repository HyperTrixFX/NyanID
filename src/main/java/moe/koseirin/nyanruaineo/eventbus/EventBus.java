package moe.koseirin.nyanruaineo.eventbus;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import moe.koseirin.nyanruaineo.eventbus.Interface.Cancellable;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventBusInterface;
import moe.koseirin.nyanruaineo.eventbus.Interface.EventHeader;
import moe.koseirin.nyanruaineo.eventbus.Interface.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
public class EventBus implements EventBusInterface {

    private static final Logger log = LoggerFactory.getLogger(EventBus.class);
    // 事件类型 -> 监听器列表（按优先级排序）
    private final Map<Class<?>, List<RegisteredListener>> listeners = new ConcurrentHashMap<>();
    private final TaskExecutor taskExecutor;
    private ExecutorService defaultExecutor;

    public EventBus(@Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void init() {
        log.info("EventBus init");
        if (taskExecutor == null) {
            defaultExecutor = Executors.newCachedThreadPool();
        }
    }

    @PreDestroy
    public void destroy() {
        if (defaultExecutor != null) {
            defaultExecutor.shutdown();
        }
    }

    @Override
    public void register(Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHeader annotation = method.getAnnotation(EventHeader.class);
            if (annotation == null) continue;

            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1) {
                log.warn("Method {} in {} has @Subscribe but does not have exactly one parameter, ignored",
                        method.getName(), listener.getClass().getName());
                continue;
            }
            Class<?> eventType = paramTypes[0];
            method.setAccessible(true);

            RegisteredListener reg = new RegisteredListener(listener, method, annotation.priority());

            listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(reg);
            // 按优先级重新排序
            listeners.get(eventType).sort(Comparator.comparingInt(r -> r.priority().ordinal()));
        }
    }

    @Override
    public void unregister(Object listener) {
        listeners.values().forEach(list -> list.removeIf(reg -> reg.listener() == listener));
    }

    @Override
    public <T> T post(T event) {
        Class<?> eventType = event.getClass();
        List<RegisteredListener> handlers = listeners.get(eventType);
        if (handlers == null) return event;

        for (RegisteredListener handler : handlers) {
            // 处理取消逻辑：如果事件可取消且已被取消，并且当前监听器不是 MONITOR，则跳过
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled()
                    && handler.priority() != Priority.MONITOR) {
                continue;
            }
            try {
                handler.invoke(event);
            } catch (Exception e) {
                log.error("Exception in event handler for event: {}", event, e);
            }
        }
        return event;
    }

    @Override
    public <T> void postAsync(T event) {
        Runnable task = () -> post(event);
        if (taskExecutor != null) {
            taskExecutor.execute(task);
        } else if (defaultExecutor != null) {
            defaultExecutor.execute(task);
        } else {
            //直接同步执行
            post(event);
        }
    }

        /**
         * 内部类：封装监听器信息
         */
        private record RegisteredListener(Object listener, Method method, Priority priority) {

        public void invoke(Object event) throws Exception {
                method.invoke(listener, event);
            }

        }
}