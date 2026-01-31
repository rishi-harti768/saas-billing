package org.gb.billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for async task execution.
 * Enables @Async annotation for background tasks like email sending.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
