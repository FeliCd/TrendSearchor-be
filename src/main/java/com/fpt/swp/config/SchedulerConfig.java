package com.fpt.swp.config;

import com.fpt.swp.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import com.fpt.swp.repository.ApiDataSourceRepository;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig implements SchedulingConfigurer {

    private final ApiDataSourceRepository apiDataSourceRepository;
    private final DataSyncService dataSyncService;

    @Bean
    public CommandLineRunner initData(DataSyncService dataSyncService) {
        return args -> {
            log.info("Initializing default API data source...");
            dataSyncService.initializeDefaultDataSource();
        };
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            () -> {
                log.info("Starting scheduled data sync...");
                try {
                    dataSyncService.triggerFullSync(50);
                    log.info("Scheduled sync completed successfully.");
                } catch (Exception e) {
                    log.error("Scheduled sync failed: {}", e.getMessage());
                }
            },
            triggerContext -> {
                String cron = apiDataSourceRepository.findBySourceName("OPENALEX")
                        .map(source -> source.getSyncSchedule())
                        .orElse("0 0 2 * * ?");
                if (cron == null || cron.isBlank()) {
                    cron = "0 0 2 * * ?";
                }
                CronTrigger trigger = new CronTrigger(cron);
                return trigger.nextExecution(triggerContext);
            }
        );
    }
}
