package com.crocodile.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * AsyncConfig - Configuration for asynchronous task execution
 * 
 * This configuration enables Spring's @Async annotation and provides
 * a custom ThreadPoolTaskExecutor for handling asynchronous word pool refills.
 * 
 * Thread Pool Configuration:
 * - Core Pool Size: 2 threads (minimum threads always alive)
 * - Max Pool Size: 5 threads (maximum threads during high load)
 * - Queue Capacity: 100 tasks (tasks waiting for execution)
 * - Thread Name Prefix: "word-pool-refill-" (for easy debugging)
 * 
 * This ensures that word pool refills happen in the background without
 * blocking the main request threads.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Create a ThreadPoolTaskExecutor for asynchronous word pool operations
     * 
     * @return configured executor
     */
    @Bean(name = "wordPoolTaskExecutor")
    public Executor wordPoolTaskExecutor() {
        log.info("Initializing WordPool TaskExecutor");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - minimum number of threads to keep alive
        executor.setCorePoolSize(2);
        
        // Maximum pool size - maximum number of threads
        executor.setMaxPoolSize(5);
        
        // Queue capacity - number of tasks that can wait for execution
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easy identification in logs
        executor.setThreadNamePrefix("word-pool-refill-");
        
        // Reject tasks when pool and queue are full (caller-runs policy)
        executor.setRejectedExecutionHandler(
            (runnable, taskExecutor) -> {
                log.warn("Word pool refill task rejected - pool and queue are full. " +
                         "Running in caller thread as fallback.");
                runnable.run();
            }
        );
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("WordPool TaskExecutor initialized with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                 executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        
        return executor;
    }
}

