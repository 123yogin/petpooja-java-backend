package com.example.restrosuite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background scheduled tasks
 * Add your scheduled jobs here
 */
@Service
public class ScheduledTasksService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);

    /**
     * Example: Run every hour
     * Cron format: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour at minute 0
    public void hourlyTask() {
        log.info("Hourly scheduled task executed");
        // Add your hourly tasks here (e.g., refresh materialized views)
    }

    /**
     * Example: Run daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void dailyTask() {
        log.info("Daily scheduled task executed");
        // Add your daily tasks here (e.g., generate reports, cleanup old data)
    }

    /**
     * Example: Run every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void frequentTask() {
        // Add frequent tasks here (e.g., sync aggregator orders)
        // Commented out to avoid log spam
        // log.debug("Frequent task executed");
    }
}

