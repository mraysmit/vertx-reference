package dev.mars.vertx.common.util;

import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for configuring thread pools in Vert.x applications.
 * Helps optimize thread pool sizes based on application workload.
 */
public class ThreadPoolConfig {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolConfig.class);
    
    // Default values
    private static final int DEFAULT_EVENT_LOOP_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_WORKER_POOL_SIZE = 20;
    private static final int DEFAULT_INTERNAL_BLOCKING_POOL_SIZE = 20;
    private static final int DEFAULT_MAX_WORKER_EXECUTE_TIME = 60 * 1000; // 60 seconds
    
    /**
     * Configures thread pools with default settings based on available processors.
     * 
     * @param options the Vert.x options to configure
     * @return the configured Vert.x options
     */
    public static VertxOptions configureDefaults(VertxOptions options) {
        logger.info("Configuring thread pools with default settings");
        
        return options
                .setEventLoopPoolSize(DEFAULT_EVENT_LOOP_POOL_SIZE)
                .setWorkerPoolSize(DEFAULT_WORKER_POOL_SIZE)
                .setInternalBlockingPoolSize(DEFAULT_INTERNAL_BLOCKING_POOL_SIZE)
                .setMaxWorkerExecuteTime(DEFAULT_MAX_WORKER_EXECUTE_TIME * 1000000L); // Convert to nanoseconds
    }
}