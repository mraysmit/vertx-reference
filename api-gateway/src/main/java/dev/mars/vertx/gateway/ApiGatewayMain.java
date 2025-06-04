package dev.mars.vertx.gateway;

// Temporarily commented out for testing
// import dev.mars.vertx.common.metrics.MetricsManager;
// import dev.mars.vertx.common.util.ShutdownManager;
// import dev.mars.vertx.common.util.ThreadPoolConfig;
import dev.mars.vertx.common.config.ConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the API Gateway.
 * Configures and starts the Vert.x instance and deploys the API Gateway verticle.
 */
public class ApiGatewayMain {
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayMain.class);

    public static void main(String[] args) {
        logger.info("Starting API Gateway");

        // Configure Vert.x options with metrics and thread pools
        VertxOptions options = new VertxOptions();
        // Temporarily commented out for testing
        // ThreadPoolConfig.configureDefaults(options);
        // MetricsManager.configureMetrics(options, true);

        // Create Vert.x instance
        Vertx vertx = Vertx.vertx(options);

        // Set up graceful shutdown
        // Temporarily commented out for testing
        // ShutdownManager shutdownManager = new ShutdownManager(vertx);
        // shutdownManager.registerShutdownHook();

        // Initialize the application
        initializeApplication(vertx);
    }

    /**
     * Initializes the application by loading configuration and deploying the API Gateway verticle.
     * Extracted to a separate method for better testability.
     *
     * @param vertx the Vertx instance
     */
    private static void initializeApplication(Vertx vertx) {
        // Load configuration from config.yaml
        ConfigLoader.load(vertx, "api-gateway/src/main/resources/config.yaml")
            .onSuccess(config -> {
                logger.info("Configuration loaded successfully");

                // Deploy the API Gateway verticle with the loaded configuration
                DeploymentOptions deploymentOptions = new DeploymentOptions()
                        .setConfig(config);

                vertx.deployVerticle(ApiGatewayVerticle.class.getName(), deploymentOptions, ar -> {
                    if (ar.succeeded()) {
                        logger.info("API Gateway verticle deployed successfully: {}", ar.result());
                    } else {
                        logger.error("Failed to deploy API Gateway verticle", ar.cause());
                        vertx.close();
                    }
                });
            })
            .onFailure(err -> {
                logger.error("Failed to load configuration", err);
                vertx.close();
            });
    }

    /**
     * Initializes the application for testing purposes.
     * This method is used by tests to verify the initialization logic without starting a new Vertx instance.
     *
     * @param vertx the Vertx instance to use
     */
    static void initializeForTesting(Vertx vertx) {
        logger.info("Initializing API Gateway for testing");

        // Load configuration from a test-specific path
        ConfigLoader.load(vertx, "api-gateway/src/test/resources/test-config.yaml")
            .onSuccess(config -> {
                logger.info("Test configuration loaded successfully");

                // In test mode, we don't actually deploy the verticle
                logger.info("Test initialization completed successfully");
            })
            .onFailure(err -> {
                logger.error("Failed to load test configuration", err);
            });
    }
}
