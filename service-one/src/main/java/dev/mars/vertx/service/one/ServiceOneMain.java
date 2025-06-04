package dev.mars.vertx.service.one;
import dev.mars.vertx.common.config.ConfigLoader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for Service One.
 * Configures and starts the Vert.x instance and deploys the Service One verticle.
 */
public class ServiceOneMain {
    private static final Logger logger = LoggerFactory.getLogger(ServiceOneMain.class);

    public static void main(String[] args) {
        logger.info("Starting Service One");

        // Create Vert.x instance with default options
        VertxOptions options = new VertxOptions();
        Vertx vertx = Vertx.vertx(options);

        // Load configuration from config.yaml
        ConfigLoader.load(vertx, "service-one/src/main/resources/config.yaml")
            .onSuccess(config -> {
                logger.info("Configuration loaded successfully");

                // Deploy the Service One verticle with the loaded configuration
                DeploymentOptions deploymentOptions = new DeploymentOptions()
                        .setConfig(config);

                vertx.deployVerticle(ServiceOneVerticle.class.getName(), deploymentOptions, ar -> {
                    if (ar.succeeded()) {
                        logger.info("Service One verticle deployed successfully: {}", ar.result());
                    } else {
                        logger.error("Failed to deploy Service One verticle", ar.cause());
                        vertx.close();
                    }
                });
            })
            .onFailure(err -> {
                logger.error("Failed to load configuration", err);
                vertx.close();
            });
    }
}
