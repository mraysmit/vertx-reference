package dev.mars.vertx.gateway;

import dev.mars.vertx.gateway.handler.ServiceHandler;
import dev.mars.vertx.gateway.router.RouterFactory;
import dev.mars.vertx.gateway.service.MicroserviceClient;
import dev.mars.vertx.gateway.service.MicroserviceClientFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Simplified API Gateway Verticle.
 * Handles HTTP requests and routes them to the appropriate microservices.
 */
public class ApiGatewayVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayVerticle.class);

    private HttpServer server;
    private MicroserviceClientFactory clientFactory;
    private RouterFactory routerFactory;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting API Gateway Verticle");

        // Initialize factories with simplified configuration
        clientFactory = new MicroserviceClientFactory(vertx, config());
        routerFactory = new RouterFactory(vertx, config());

        // Create route handlers map
        Map<String, Handler<RoutingContext>> handlers = createHandlers();

        // Create the router with the handlers
        routerFactory.createRouter(handlers)
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

    /**
     * Creates the handlers for all routes.
     *
     * @return a map of path patterns to handlers
     */
    private Map<String, Handler<RoutingContext>> createHandlers() {
        Map<String, Handler<RoutingContext>> handlers = new HashMap<>();

        // Get service clients
        MicroserviceClient serviceOneClient = clientFactory.getClient("service-one");
        MicroserviceClient serviceTwoClient = clientFactory.getClient("service-two");

        // Documentation endpoints
        // OpenAPI documentation
        handlers.put("/openapi.yaml", ctx -> {
            // Read the OpenAPI YAML file from the resources directory
            vertx.fileSystem().readFile("src/main/resources/openapi.yaml", ar -> {
                if (ar.succeeded()) {
                    ctx.response()
                            .putHeader("Content-Type", "text/yaml")
                            .end(ar.result());
                } else {
                    logger.error("Failed to read OpenAPI documentation", ar.cause());
                    ctx.fail(500, ar.cause());
                }
            });
        });

        // Swagger UI redirect
        handlers.put("/swagger-ui", ctx -> {
            ctx.response()
                    .setStatusCode(302)
                    .putHeader("Location", "/swagger-ui/index.html?url=/openapi.yaml")
                    .end();
        });

        // Docs redirect to Swagger UI
        handlers.put("/docs", ctx -> {
            ctx.response()
                    .setStatusCode(302)
                    .putHeader("Location", "/swagger-ui")
                    .end();
        });

        // Health check handler
        handlers.put("/health", ctx -> {
            JsonObject health = new JsonObject()
                    .put("status", "UP")
                    .put("timestamp", System.currentTimeMillis());

            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .end(health.encode());
        });

        // Service One routes
        handlers.put("GET:/api/service-one", new ServiceHandler(serviceOneClient, "service-one"));
        handlers.put("POST:/api/service-one", new ServiceHandler(serviceOneClient, "service-one"));
        handlers.put("GET:/api/service-one/:id", new ServiceHandler(serviceOneClient, "service-one"));
        handlers.put("PUT:/api/service-one/:id", new ServiceHandler(serviceOneClient, "service-one"));
        handlers.put("DELETE:/api/service-one/:id", new ServiceHandler(serviceOneClient, "service-one"));

        // Service Two routes - using the request transformer to set the action
        handlers.put("GET:/api/service-two/random", new ServiceHandler(serviceTwoClient, "service-two"));
        handlers.put("GET:/api/service-two/forecast/:city", new ServiceHandler(serviceTwoClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", "forecast");
            return request;
        }, "service-two"));
        handlers.put("GET:/api/service-two/cities", new ServiceHandler(serviceTwoClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", "cities");
            return request;
        }, "service-two"));
        handlers.put("GET:/api/service-two/stats", new ServiceHandler(serviceTwoClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", "stats");
            return request;
        }, "service-two"));
        handlers.put("GET:/api/service-two/:id", new ServiceHandler(serviceTwoClient, "service-two"));
        handlers.put("POST:/api/service-two", new ServiceHandler(serviceTwoClient, "service-two"));

        return handlers;
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
