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
        return configure(
                options,
                DEFAULT_EVENT_LOOP_POOL_SIZE,
                DEFAULT_WORKER_POOL_SIZE,
                DEFAULT_INTERNAL_BLOCKING_POOL_SIZE,
                DEFAULT_MAX_WORKER_EXECUTE_TIME
        );
    }
    
    /**
     * Configures thread pools with custom settings.
     * 
     * @param options the Vert.x options to configure
     * @param eventLoopPoolSize the number of event loop threads
     * @param workerPoolSize the number of worker threads
     * @param internalBlockingPoolSize the number of internal blocking threads
     * @param maxWorkerExecuteTime the maximum time a worker task can execute in milliseconds
     * @return the configured Vert.x options
     */
    public static VertxOptions configure(
            VertxOptions options,
            int eventLoopPoolSize,
            int workerPoolSize,
            int internalBlockingPoolSize,
            int maxWorkerExecuteTime
    ) {
        logger.info("Configuring thread pools: eventLoopPoolSize={}, workerPoolSize={}, internalBlockingPoolSize={}, maxWorkerExecuteTime={}ms",
                eventLoopPoolSize, workerPoolSize, internalBlockingPoolSize, maxWorkerExecuteTime);
        
        return options
                .setEventLoopPoolSize(eventLoopPoolSize)
                .setWorkerPoolSize(workerPoolSize)
                .setInternalBlockingPoolSize(internalBlockingPoolSize)
                .setMaxWorkerExecuteTime(maxWorkerExecuteTime * 1000000L); // Convert to nanoseconds
    }
    
    /**
     * Calculates the optimal event loop pool size based on available processors.
     * 
     * @return the optimal event loop pool size
     */
    public static int calculateOptimalEventLoopPoolSize() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int optimal = 2 * availableProcessors;
        logger.info("Calculated optimal event loop pool size: {} (based on {} available processors)", 
                optimal, availableProcessors);
        return optimal;
    }
    
    /**
     * Calculates the optimal worker pool size based on available processors and expected I/O ratio.
     * 
     * @param ioRatio the ratio of I/O operations to CPU operations (higher means more I/O intensive)
     * @return the optimal worker pool size
     */
    public static int calculateOptimalWorkerPoolSize(double ioRatio) {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int optimal = (int) Math.ceil(availableProcessors * (1 + ioRatio));
        logger.info("Calculated optimal worker pool size: {} (based on {} available processors and I/O ratio of {})", 
                optimal, availableProcessors, ioRatio);
        return optimal;
    }
}