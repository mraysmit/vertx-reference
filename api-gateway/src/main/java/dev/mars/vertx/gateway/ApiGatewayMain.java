package dev.mars.vertx.gateway;

// Temporarily commented out for testing
// import dev.mars.vertx.common.metrics.MetricsManager;
// import dev.mars.vertx.common.util.ShutdownManager;
// import dev.mars.vertx.common.util.ThreadPoolConfig;
// import dev.mars.vertx.common.metrics.MetricsManager;
// import dev.mars.vertx.common.util.ShutdownManager;
// import dev.mars.vertx.common.util.ThreadPoolConfig;
// import dev.mars.vertx.common.metrics.MetricsManager;
// import dev.mars.vertx.common.util.ShutdownManager;
// import dev.mars.vertx.common.util.ThreadPoolConfig;
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

        // Deploy the API Gateway verticle
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", 8080)
                        .put("service.one.address", "service.one")
                        .put("service.two.address", "service.two")
                );

        vertx.deployVerticle(ApiGatewayVerticle.class.getName(), deploymentOptions, ar -> {
            if (ar.succeeded()) {
                logger.info("API Gateway verticle deployed successfully: {}", ar.result());
            } else {
                logger.error("Failed to deploy API Gateway verticle", ar.cause());
                vertx.close();
            }
        });
    }
}
