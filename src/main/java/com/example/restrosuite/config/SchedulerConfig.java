package com.example.restrosuite.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {
    // Spring Scheduler is now enabled
    // Use @Scheduled annotation in service classes for background jobs
}

