package moe.koseirin.nyanruaineo.utils.System;

/*
 * @author KoseiRin_
 * awa
 */

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;


/**
 * Initializes the Quartz Scheduler.
 */
@Component
public class QuartzStarter {



    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        return new SchedulerFactoryBean();
    }

    @Bean
    public Scheduler scheduler(SchedulerFactoryBean factory) {
        return factory.getScheduler();  // exposes the Scheduler instance
    }
}