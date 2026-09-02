package moe.koseirin.nyanruaineo;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;

import java.util.concurrent.ConcurrentHashMap;

public class FakeSystemConfigCacheService extends SystemConfigCacheService {

    private final ConcurrentHashMap<String, String> configMap = new ConcurrentHashMap<>();
    private boolean throwOnGet = false;
    private RuntimeException exceptionToThrow = null;
    private long updateDelayMillis = 0;   // 新增延迟字段

    public FakeSystemConfigCacheService() {
        super(null);
    }

    // 设置 updateConfig 的延迟（毫秒）
    public void setUpdateDelay(long millis) {
        this.updateDelayMillis = millis;
    }

    @Override
    public String getConfig(String key) {
        if (throwOnGet && exceptionToThrow != null) {
            throw exceptionToThrow;
        }
        return configMap.get(key);
    }

    @Override
    public void updateConfig(String key, String newValue) {
        if (updateDelayMillis > 0) {
            try {
                Thread.sleep(updateDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        configMap.put(key, newValue);
    }

    @Override
    public void loadConfigs() {
        // 模拟重新加载，但这里无需操作
    }

    // 测试辅助方法
    public void setThrowOnGet(RuntimeException exception) {
        this.throwOnGet = true;
        this.exceptionToThrow = exception;
    }

    public void clearThrowOnGet() {
        this.throwOnGet = false;
        this.exceptionToThrow = null;
    }

    public void putConfig(String key, String value) {
        configMap.put(key, value);
    }
}