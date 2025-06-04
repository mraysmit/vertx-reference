package dev.mars.vertx.common.util;

import dev.mars.vertx.common.config.ConfigLoader;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
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
     * @param vertx the Vertx instance
     * @param options the Vert.x options to configure
     * @return a Future with the configured Vert.x options
     */
    public static Future<VertxOptions> configureDefaults(Vertx vertx, VertxOptions options) {
        return ConfigLoader.load(vertx, "common-core/src/main/resources/config.yaml")
            .map(config -> {
                logger.info("Loaded thread pool configuration from config.yaml");

                // Get thread pool configuration from loaded config
                JsonObject threadPoolsConfig = config.getJsonObject("thread-pools", new JsonObject());

                // Get event loop pool size
                int eventLoopPoolSize = threadPoolsConfig.getJsonObject("event-loop", new JsonObject())
                        .getInteger("size", 0);
                if (eventLoopPoolSize <= 0) {
                    eventLoopPoolSize = DEFAULT_EVENT_LOOP_POOL_SIZE;
                    logger.info("Using calculated event loop pool size: {}", eventLoopPoolSize);
                }

                // Get worker pool size and max execute time
                JsonObject workerConfig = threadPoolsConfig.getJsonObject("worker", new JsonObject());
                int workerPoolSize = workerConfig.getInteger("size", DEFAULT_WORKER_POOL_SIZE);
                int maxWorkerExecuteTime = workerConfig.getInteger("max-execute-time", DEFAULT_MAX_WORKER_EXECUTE_TIME);

                // Get internal blocking pool size
                int internalBlockingPoolSize = threadPoolsConfig.getJsonObject("internal-blocking", new JsonObject())
                        .getInteger("size", DEFAULT_INTERNAL_BLOCKING_POOL_SIZE);

                return configure(
                        options,
                        eventLoopPoolSize,
                        workerPoolSize,
                        internalBlockingPoolSize,
                        maxWorkerExecuteTime
                );
            })
            .recover(err -> {
                logger.warn("Failed to load thread pool configuration from config.yaml, using defaults", err);
                return Future.succeededFuture(configure(
                        options,
                        DEFAULT_EVENT_LOOP_POOL_SIZE,
                        DEFAULT_WORKER_POOL_SIZE,
                        DEFAULT_INTERNAL_BLOCKING_POOL_SIZE,
                        DEFAULT_MAX_WORKER_EXECUTE_TIME
                ));
            });
    }

    /**
     * Configures thread pools with default settings based on available processors.
     * This is a synchronous version that uses hard-coded defaults.
     *
     * @param options the Vert.x options to configure
     * @return the configured Vert.x options
     * @deprecated Use {@link #configureDefaults(Vertx, VertxOptions)} instead
     */
    @Deprecated
    public static VertxOptions configureDefaults(VertxOptions options) {
        logger.warn("Using deprecated synchronous configureDefaults method with hard-coded values");
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
