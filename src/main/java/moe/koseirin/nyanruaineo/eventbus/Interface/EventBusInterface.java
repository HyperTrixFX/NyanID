package moe.koseirin.nyanruaineo.eventbus.Interface;

/*
 * @author KoseiRin_
 * awa
 */

public interface EventBusInterface {
    /**
     * 注册监听器对象（其 @Subscribe 方法将被扫描）
     */
    void register(Object listener);

    /**
     * 注销监听器对象
     */
    void unregister(Object listener);

    /**
     * 同步发布事件
     * @return 事件本身（可用于检查取消状态）
     */
    <T> T post(T event);

    /**
     * 异步发布事件（使用默认线程池）
     */
    <T> void postAsync(T event);
}