package dev.mars.vertx.gateway;

import dev.mars.vertx.gateway.handler.HealthCheckHandler;
import dev.mars.vertx.gateway.handler.ServiceOneHandler;
import dev.mars.vertx.gateway.handler.ServiceTwoHandler;
import dev.mars.vertx.gateway.router.RouterFactory;
import dev.mars.vertx.gateway.service.MicroserviceClientFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API Gateway Verticle.
 * Handles HTTP requests and routes them to the appropriate microservices.
 * Uses a modular design with separate components for different concerns.
 */
public class ApiGatewayVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayVerticle.class);

    private HttpServer server;
    private MicroserviceClientFactory clientFactory;
    private RouterFactory routerFactory;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting API Gateway Verticle");

        // Initialize factories
        clientFactory = new MicroserviceClientFactory(vertx, config());
        routerFactory = new RouterFactory(vertx, config());

        // Create handlers
        HealthCheckHandler healthCheckHandler = new HealthCheckHandler();
        ServiceOneHandler serviceOneHandler = new ServiceOneHandler(clientFactory.getClient("service-one"));
        ServiceTwoHandler serviceTwoHandler = new ServiceTwoHandler(clientFactory.getClient("service-two"));

        // Create the router
        routerFactory.createRouter(
                healthCheckHandler,
                serviceOneHandler,
                serviceTwoHandler)
            .onSuccess(router -> {
                // Start the HTTP server
                int port = config().getInteger("http.port", 8080);
                server = vertx.createHttpServer();

                server.requestHandler(router)
                    .listen(port, ar -> {
                        if (ar.succeeded()) {
                            logger.info("HTTP server started on port {}", port);
                            startPromise.complete();
                        } else {
                            logger.error("Failed to start HTTP server", ar.cause());
                            startPromise.fail(ar.cause());
                        }
                    });
            })
            .onFailure(err -> {
                logger.error("Failed to create router", err);
                startPromise.fail(err);
            });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("Stopping API Gateway Verticle");

        if (server != null) {
            server.close(ar -> {
                if (ar.succeeded()) {
                    logger.info("HTTP server closed successfully");
                    stopPromise.complete();
                } else {
                    logger.error("Failed to close HTTP server", ar.cause());
                    stopPromise.fail(ar.cause());
                }
            });
        } else {
            stopPromise.complete();
        }
    }
}
