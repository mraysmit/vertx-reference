package dev.mars.vertx.service.one;
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

        // Deploy the Service One verticle
        DeploymentOptions deploymentOptions = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", "service.one")
                        .put("http.port", 8081) // In case we want to expose an HTTP endpoint
                );

        vertx.deployVerticle(ServiceOneVerticle.class.getName(), deploymentOptions, ar -> {
            if (ar.succeeded()) {
                logger.info("Service One verticle deployed successfully: {}", ar.result());
            } else {
                logger.error("Failed to deploy Service One verticle", ar.cause());
                vertx.close();
            }
        });
    }
}
