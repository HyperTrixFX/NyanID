package moe.koseirin.nyanruaineo.utils.System;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.entity.SystemConfig;
import moe.koseirin.nyanruaineo.repository.SystemConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/*
 * @author KoseiRin_
 * awa
 */

@Slf4j
@Service
public class SystemConfigCacheService {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigCacheService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }
    private final ConcurrentHashMap<String, String> configMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void loadConfigs() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        configMap.clear();
        configs.forEach(c -> configMap.put(c.getConfigKey(), c.getConfigValue()));
        log.info("Loaded {} configs into cache.", configMap.size());
    }
    public String getConfig(String key) {
        return configMap.get(key);
    }


    @Transactional
    public void updateConfig(String key, String newValue) {
        SystemConfig config = systemConfigRepository.findById(key)
                .orElseGet(() -> {
                    log.warn("Config '{}' not found, will create new.", key);
                    return new SystemConfig(key, newValue);
                });
        config.setConfigValue(newValue);
        systemConfigRepository.save(config);
        registerPostCommit(() -> configMap.put(key, newValue));
    }


    @Transactional
    public void addConfig(String key, String value) {
        if (systemConfigRepository.existsById(key)) {
            throw new IllegalArgumentException("Config already exists: " + key);
        }
        SystemConfig config = new SystemConfig(key, value);
        systemConfigRepository.save(config);
        registerPostCommit(() -> configMap.put(key, value));
    }

    @Transactional
    public void deleteConfig(String key) {
        if (!systemConfigRepository.existsById(key)) {
            log.warn("Config '{}' does not exist, deletion ignored.", key);
            return;
        }
        systemConfigRepository.deleteById(key);
        registerPostCommit(() -> configMap.remove(key));
    }

    private void registerPostCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                }
        );
    }

    public ConcurrentHashMap<String, String> getAllConfigs() {
        return new ConcurrentHashMap<>(configMap);
    }



}
