package dev.mars.vertx.common.util;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for managing graceful shutdown of Vert.x applications.
 * Ensures proper cleanup of resources and orderly shutdown of verticles.
 */
public class ShutdownManager {
    private static final Logger logger = LoggerFactory.getLogger(ShutdownManager.class);
    
    private final Vertx vertx;
    
    /**
     * Creates a new ShutdownManager.
     * 
     * @param vertx the Vertx instance to manage
     */
    public ShutdownManager(Vertx vertx) {
        this.vertx = vertx;
        logger.info("ShutdownManager created");
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
     * Closes the Vertx instance.
     */
    public void shutdown() {
        logger.info("Starting graceful shutdown sequence");
        
        vertx.close(ar -> {
            if (ar.succeeded()) {
                logger.info("Vertx closed successfully");
            } else {
                logger.error("Error closing Vertx", ar.cause());
            }
        });
        
        logger.info("Graceful shutdown completed");
    }
}