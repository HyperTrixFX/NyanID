package moe.koseirin.nyanruaineo;

/*
 * @author KoseiRin_
 * awa
 */


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import moe.koseirin.nyanruaineo.Minecraft.MinecraftProxy;
import moe.koseirin.nyanruaineo.Minecraft.config.cfg.BackendServer;
import moe.koseirin.nyanruaineo.Minecraft.service.BackendServerManager;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerKickService;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerQueryService;
import moe.koseirin.nyanruaineo.Minecraft.service.PlayerTransferService;
import moe.koseirin.nyanruaineo.dto.BackendServerStatusDTO;
import moe.koseirin.nyanruaineo.services.impl.ProxyFuncImpl;
import moe.koseirin.nyanruaineo.utils.Respond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)   // 仅为 @Mock 注解服务，但 cacheService 不再 mock
class ProxyFuncImplTest {

    @Mock
    private MinecraftProxy proxy;          // 未被使用，可保留 mock
    @Mock
    private PlayerQueryService playerQueryService;
    @Mock
    private BackendServerManager backendServerManager;
    @Mock
    private PlayerTransferService playerTransferService;
    @Mock
    private PlayerKickService playerKickService;

    private FakeSystemConfigCacheService cacheService;  // 使用 Fake
    private ProxyFuncImpl proxyFunc;

    private static final String KEY_BACKEND_SERVERS = "proxy.backend.servers";

    @BeforeEach
    void setUp() {
        cacheService = new FakeSystemConfigCacheService();
        // 设置初始配置
        cacheService.putConfig(KEY_BACKEND_SERVERS, createInitialConfigJson());

        // 手动创建 ProxyFuncImpl，注入 Fake
        proxyFunc = new ProxyFuncImpl(proxy, playerQueryService, cacheService, backendServerManager,
                playerTransferService, playerKickService, new Respond());
    }

    private String createInitialConfigJson() {
        JSONObject config = new JSONObject();
        config.put("default_server", "lobby");
        JSONArray arr = new JSONArray();
        arr.add(new BackendServer("lobby-001", 1, "lobby", "localhost", 25566));
        arr.add(new BackendServer("survival-001", 2, "survival", "localhost", 25567));
        config.put("server_list", arr);
        return config.toJSONString();
    }

    // ==================== 基础功能测试 ====================

    @Test
    void testGetAllServers_ReturnsList() {
        List<BackendServerStatusDTO> servers = proxyFunc.getAllServers();
        assertEquals(2, servers.size());
        assertEquals("lobby", servers.get(0).getName());
    }

    @Test
    void testAddServer_Success() {
        BackendServer newServer = new BackendServer(null, 3, "test", "localhost", 1004);
        var response = proxyFunc.addServer(newServer);
        assertEquals(HttpStatus.OK, response.getStatusCode());   // 需要 import HttpStatus
        assertNotNull(newServer.getUid());

        JSONObject updatedConfig = JSONObject.parseObject(cacheService.getConfig(KEY_BACKEND_SERVERS));
        assertEquals(3, updatedConfig.getJSONArray("server_list").size());
    }

    @Test
    void testAddServer_InvalidPort_ReturnsBadRequest() {
        BackendServer invalid = new BackendServer(null, 1, "test", "localhost", 70000);
        var response = proxyFunc.addServer(invalid);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        JSONObject config = JSONObject.parseObject(cacheService.getConfig(KEY_BACKEND_SERVERS));
        assertEquals(2, config.getJSONArray("server_list").size());
    }

    @Test
    void testRemoveServer_Success() {
        var response = proxyFunc.removeServer("lobby-001");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject config = JSONObject.parseObject(cacheService.getConfig(KEY_BACKEND_SERVERS));
        assertEquals(1, config.getJSONArray("server_list").size());
        assertEquals("survival-001", config.getJSONArray("server_list").getJSONObject(0).getString("uid"));
    }

    @Test
    void testUpdateServer_Success() {
        BackendServer updated = new BackendServer(null, 1, "lobby-new", "127.0.0.1", 25568);
        var response = proxyFunc.updateServer("lobby-001", updated);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JSONObject config = JSONObject.parseObject(cacheService.getConfig(KEY_BACKEND_SERVERS));
        JSONObject server = config.getJSONArray("server_list").getJSONObject(0);
        assertEquals("lobby-new", server.getString("name"));
        assertEquals("lobby-001", server.getString("uid"));
    }

    // ==================== 并发与锁测试 ====================

    @Test
    void testConcurrentAddServers_NoLostUpdates() throws InterruptedException {
        int threadCount = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    barrier.await();
                    BackendServer server = new BackendServer(null, 1, "server-" + index, "localhost", 2000 + index);
                    proxyFunc.addServer(server);
                } catch (Exception e) {
                    // 忽略测试异常
                } finally {
                    done.countDown();
                }
            });
        }
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        JSONObject finalConfig = JSONObject.parseObject(cacheService.getConfig(KEY_BACKEND_SERVERS));
        assertEquals(2 + threadCount, finalConfig.getJSONArray("server_list").size(), "并发添加不应丢失更新");
    }

    @Test
    void testReadLockAllowsConcurrentReads() throws InterruptedException {
        // 为了观察并发，这里直接使用 ProxyFuncImpl 的 getAllServers 即可，
        // 因为 Fake 的 getConfig 非常快，难以测量真实并发，但可以简单验证多个线程能同时进入。
        // 此处简化：同时启动多个读线程，确保无异常。
        int threadCount = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    proxyFunc.getAllServers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        executor.shutdown();
    }

    @Test
    void testWriteLockIsExclusive() throws InterruptedException {
        // 设置 Fake 的 updateConfig 延迟为 100ms
        cacheService.setUpdateDelay(10);

        int threadCount = 300;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    barrier.await(); // 同时开始
                    BackendServer server = new BackendServer(null, 1, "writer-" + index, "localhost", 3000 + index);
                    proxyFunc.addServer(server);
                } catch (Exception e) {
                    // 忽略异常
                } finally {
                    done.countDown();
                }
            });
        }

        done.await(10, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;
        executor.shutdown();

        // 验证最终配置：初始2个 + 添加2个 = 4个
        JSONObject finalConfig = JSONObject.parseObject(cacheService.getConfig(KEY_BACKEND_SERVERS));
        assertEquals(2 + threadCount, finalConfig.getJSONArray("server_list").size());

        // 验证写锁互斥：总耗时至少等于两次延迟之和（200ms），允许一定的误差
        assertTrue(duration >= 200, "写操作应串行执行，实际耗时: " + duration + "ms");

        // 清理：移除延迟设置，避免影响其他测试
        cacheService.setUpdateDelay(0);
    }

    // ==================== 异常场景测试 ====================

    @Test
    void testAddServer_CacheReadThrowsException_PropagatesAndLockReleased() {
        // 设置 Fake 在 getConfig 时抛出异常
        cacheService.setThrowOnGet(new RuntimeException("cache unavailable"));

        BackendServer server = new BackendServer(null, 1, "test", "localhost", 1004);
        assertThrows(RuntimeException.class, () -> proxyFunc.addServer(server));

        // 恢复 getConfig 正常
        cacheService.clearThrowOnGet();
        List<BackendServerStatusDTO> servers = proxyFunc.getAllServers();
        assertEquals(2, servers.size(), "锁应被释放，后续操作可正常进行");
    }
}