package moe.koseirin.nyanruaineo.utils;

/*
 * @author KoseiRin_
 * awa
 */

import jakarta.annotation.PostConstruct;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Component;

@Component
public class QuartzStarter {


    private final Scheduler scheduler;

    public QuartzStarter(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() throws SchedulerException {
        if (scheduler.isInStandbyMode()) {
            scheduler.start();
        }
    }
}