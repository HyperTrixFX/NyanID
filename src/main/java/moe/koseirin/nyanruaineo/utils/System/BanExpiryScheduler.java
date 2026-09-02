package moe.koseirin.nyanruaineo.utils.System;

/*
 * @author KoseiRin_
 * awa
 */

import lombok.extern.slf4j.Slf4j;
import moe.koseirin.nyanruaineo.repository.BanUserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 自动解封任务：定时把已过期的封禁批量置为失效。
 * <p>
 * 每 60 秒执行一次，仅一条 {@code UPDATE ... WHERE isActive = true AND ExpireTime <= now}，
 * 不做逐条轮询，资源占用极低。即使本任务尚未运行，登录鉴权查询本身也会忽略已过期的封禁，
 * 所以自动解封只是表数据清理，不影响正确性。
 */
@Slf4j
@Component
public class BanExpiryScheduler {

    private final BanUserRepository banUserRepository;

    public BanExpiryScheduler(BanUserRepository banUserRepository) {
        this.banUserRepository = banUserRepository;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @Transactional
    public void deactivateExpiredBans() {
        int count = banUserRepository.deactivateExpired(LocalDateTime.now());
        if (count > 0) {
            log.info("Auto-unbanned {} expired ban record(s)", count);
        }
    }
}
