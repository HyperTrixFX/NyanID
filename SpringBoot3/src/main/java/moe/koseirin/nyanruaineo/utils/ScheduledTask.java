package moe.koseirin.nyanruaineo.utils;

/*
 * @author KoseiRin_
 * awa
 */

import moe.koseirin.nyanruaineo.repository.UserDevicesRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;


@Component
public class ScheduledTask {


    private final UserDevicesRepository userDeviceRepository;

    public ScheduledTask(UserDevicesRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    @Scheduled(cron = "30 * * * * *")
    @Transactional
    public void asyncTask() {
        LocalDateTime cutoffDateTime = LocalDateTime.now()
                .minusDays(3)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        userDeviceRepository.deleteByCreateTimeBefore(cutoffDateTime);
    }
    @Scheduled(cron = "30 * * * * *")
    @Transactional
    public void asyncTask1() {
        LocalDateTime cutoffDateTime = LocalDateTime.now()
                .minusDays(2)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        userDeviceRepository.SetNosDevices(cutoffDateTime);
    }
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void asyncTask2() {

    }
}
