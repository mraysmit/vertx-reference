package dev.mars.vertx.service.two;

import dev.mars.vertx.common.metrics.MetricsManager;
import dev.mars.vertx.common.util.ShutdownManager;
import dev.mars.vertx.common.util.ThreadPoolConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Service Two.
 * Configures and starts the Vert.x instance and deploys the Service Two verticle.
 */
public class ServiceTwoMain {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTwoMain.class);
    
    public static void main(String[] args) {
        logger.info("Starting Service Two");
        
        // Configure Vert.x options with metrics and thread pools
        VertxOptions options = new VertxOptions();
        ThreadPoolConfig.configureDefaults(options);
        MetricsManager.configureMetrics(options, true);
        
        // Create Vert.x instance
        Vertx vertx = Vertx.vertx(options);
        
        // Set up graceful shutdown
        ShutdownManager shutdownManager = new ShutdownManager(vertx);
        shutdownManager.registerShutdownHook();
        
        // Deploy the Service Two verticle
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", "service.two")
                        .put("http.port", 8082) // In case we want to expose an HTTP endpoint
                );
        
        vertx.deployVerticle(ServiceTwoVerticle.class.getName(), deploymentOptions, ar -> {
            if (ar.succeeded()) {
                logger.info("Service Two verticle deployed successfully: {}", ar.result());
            } else {
                logger.error("Failed to deploy Service Two verticle", ar.cause());
                vertx.close();
            }
        });
    }
}