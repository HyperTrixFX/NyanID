package moe.koseirin.nyanruaineo.services.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerQueryService;
import moe.koseirin.nyanruaineo.utils.System.SystemConfigCacheService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * @author KoseiRin_
 * awa
 */

/**
 * 代理后端服务器的业务逻辑（增删改查）。HTTP 端点位于
 * {@code moe.koseirin.nyanruaineo.server.V3Contorller.ProxyController}，鉴权由
 * {@code PermissionService} 负责。
 */
@Service
@RequiredArgsConstructor
public class ProxyFuncImpl {

    private final MinecraftProxy proxy;
    private final PlayerQueryService playerQueryService;
    private final SystemConfigCacheService cacheService;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private static final String KEY_BACKEND_SERVERS = "proxy.backend.servers";

    @Transactional
    public List<BackendServer> getAllServers() {
        readLock.lock();
        try {
            JSONObject config = readConfig();
            JSONArray serverArray = config.getJSONArray("server_list");
            if (serverArray == null) {
                return new ArrayList<>();
            }
            return serverArray.toList(BackendServer.class);
        } finally {
            readLock.unlock();
        }
    }

    @Transactional
    public ResponseEntity<?> addServer(BackendServer newServer) {
        writeLock.lock();
        try {
            if (!validateServer(newServer)) {
                return ResponseEntity.badRequest().build();
            }

            newServer.setUid(UUID.randomUUID().toString());
            JSONObject config = readConfig();
            JSONArray serverArray = config.getJSONArray("server_list");
            if (serverArray == null) {
                serverArray = new JSONArray();
                config.put("server_list", serverArray);
            }
            serverArray.add(newServer);
            saveConfig(config);
            return ResponseEntity.ok().body(config);
        } finally {
            writeLock.unlock();
        }
    }

    @Transactional
    public ResponseEntity<?> removeServer(String uid) {
        writeLock.lock();
        try {
            JSONObject config = readConfig();
            JSONArray serverArray = config.getJSONArray("server_list");
            if (serverArray != null) {
                serverArray.removeIf(obj -> {
                    JSONObject server = (JSONObject) obj;
                    return uid.equals(server.getString("uid"));
                });
            }
            saveConfig(config);
            return ResponseEntity.ok().body(config);
        } finally {
            writeLock.unlock();
        }
    }

    @Transactional
    public ResponseEntity<?> updateServer(String uid, BackendServer updatedServer) {
        writeLock.lock();
        try {
            if (!validateServer(updatedServer)) {
                return ResponseEntity.badRequest().build();
            }
            JSONObject config = readConfig();
            JSONArray serverArray = config.getJSONArray("server_list");
            if (serverArray != null) {
                for (int i = 0; i < serverArray.size(); i++) {
                    JSONObject server = serverArray.getJSONObject(i);
                    if (uid.equals(server.getString("uid"))) {
                        server.put("name", updatedServer.getName());
                        server.put("host", updatedServer.getHost());
                        server.put("port", updatedServer.getPort());
                        server.put("priority", updatedServer.getPriority());
                        break;
                    }
                }
            }
            saveConfig(config);
            return ResponseEntity.ok().body(config);
        } finally {
            writeLock.unlock();
        }
    }

    public JSONObject readConfig() {
        String json = cacheService.getConfig(KEY_BACKEND_SERVERS);
        return JSONObject.parseObject(json);
    }

    public void saveConfig(JSONObject config) {
        cacheService.updateConfig(KEY_BACKEND_SERVERS, config.toJSONString());
        cacheService.loadConfigs();
    }

    private boolean validateServer(BackendServer server) {
        if (server == null) {
            return false;
        }
        if (server.getHost() == null || server.getHost().isBlank()) {
            return false;
        }
        if (server.getPort() < 1 || server.getPort() > 65535) {
            return false;
        }
        if (server.getName() == null || server.getName().isBlank()) {
            return false;
        }
        return server.getPriority() >= 0;
    }
}
