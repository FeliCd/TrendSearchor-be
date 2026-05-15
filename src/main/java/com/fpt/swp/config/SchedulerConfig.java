package com.fpt.swp.config;

import com.fpt.swp.service.DataSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulerConfig {

    private final DataSyncService dataSyncService;

    @Bean
    public CommandLineRunner initData(DataSyncService dataSyncService) {
        return args -> {
            log.info("Initializing default API data source...");
            dataSyncService.initializeDefaultDataSource();
        };
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledWeeklySync() {
        log.info("Starting scheduled weekly data sync...");
        try {
            dataSyncService.triggerFullSync(50);
            log.info("Scheduled sync completed successfully.");
        } catch (Exception e) {
            log.error("Scheduled sync failed: {}", e.getMessage());
        }
    }
}
