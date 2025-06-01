package dev.mars.vertx.common.util;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Utility class for managing graceful shutdown of Vert.x applications.
 * Ensures proper cleanup of resources and orderly shutdown of verticles.
 */
public class ShutdownManager {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);
    private static final int DEFAULT_SHUTDOWN_TIMEOUT_SECONDS = 30;
    
    private final Vertx vertx;
    private final List<Supplier<Future<Void>>> shutdownHooks = new ArrayList<>();
    private final int shutdownTimeoutSeconds;
    
    /**
     * Creates a new ShutdownManager with the default shutdown timeout.
     * 
     * @param vertx the Vertx instance to manage
     */
    public ShutdownManager(Vertx vertx) {
        this(vertx, DEFAULT_SHUTDOWN_TIMEOUT_SECONDS);
    }
    
    /**
     * Creates a new ShutdownManager with a custom shutdown timeout.
     * 
     * @param vertx the Vertx instance to manage
     * @param shutdownTimeoutSeconds the maximum time to wait for shutdown in seconds
     */
    public ShutdownManager(Vertx vertx, int shutdownTimeoutSeconds) {
        this.vertx = vertx;
        this.shutdownTimeoutSeconds = shutdownTimeoutSeconds;
        logger.info("ShutdownManager created with timeout of {} seconds", shutdownTimeoutSeconds);
    }
    
    /**
     * Adds a shutdown hook to be executed during shutdown.
     * 
     * @param hook a supplier that returns a Future to be completed when the hook is done
     * @return this ShutdownManager for method chaining
     */
    public ShutdownManager addShutdownHook(Supplier<Future<Void>> hook) {
        shutdownHooks.add(hook);
        logger.debug("Added shutdown hook: {}", hook);
        return this;
    }
    
    /**
     * Registers a JVM shutdown hook to trigger graceful shutdown.
     */
    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered, initiating graceful shutdown");
            shutdown();
        }));
        logger.info("JVM shutdown hook registered");
    }
    
    /**
     * Initiates a graceful shutdown of the application.
     * Executes all registered shutdown hooks and then closes the Vertx instance.
     */
    public void shutdown() {
        logger.info("Starting graceful shutdown sequence");
        
        // Create a latch to wait for shutdown to complete
        CountDownLatch latch = new CountDownLatch(1);
        
        // Execute all shutdown hooks in sequence
        Future<Void> future = Future.succeededFuture();
        for (Supplier<Future<Void>> hook : shutdownHooks) {
            future = future.compose(v -> {
                try {
                    return hook.get();
                } catch (Exception e) {
                    logger.error("Error executing shutdown hook", e);
                    return Future.succeededFuture(); // Continue with next hook even if this one fails
                }
            });
        }
        
        // Close Vertx after all hooks are executed
        future.compose(v -> {
            logger.info("All shutdown hooks executed, closing Vertx");
            return vertx.close();
        }).onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Vertx closed successfully");
            } else {
                logger.error("Error closing Vertx", ar.cause());
            }
            latch.countDown();
        });
        
        // Wait for shutdown to complete or timeout
        try {
            if (!latch.await(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {
                logger.warn("Shutdown timed out after {} seconds", shutdownTimeoutSeconds);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Shutdown interrupted", e);
        }
        
        logger.info("Graceful shutdown completed");
    }
}