package dev.mars.vertx.gateway.router;

import dev.mars.vertx.gateway.handler.HealthCheckHandler;
import dev.mars.vertx.gateway.handler.RequestHandler;
import dev.mars.vertx.gateway.handler.ServiceOneHandler;
import dev.mars.vertx.gateway.handler.ServiceTwoHandler;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.openapi.RouterBuilderOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Factory for creating and configuring routers.
 */
public class RouterFactory {
    private static final Logger logger = LoggerFactory.getLogger(RouterFactory.class);

    private final Vertx vertx;
    private final JsonObject config;

    /**
     * Creates a new router factory.
     *
     * @param vertx the Vertx instance
     * @param config the configuration
     */
    public RouterFactory(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        logger.info("Created RouterFactory");
    }

    /**
     * Creates a new router with the specified handlers.
     *
     * @param healthCheckHandler the health check handler
     * @param serviceOneHandler the service one handler
     * @param serviceTwoHandler the service two handler
     * @return the router
     */
    public Future<Router> createRouter(
            RequestHandler healthCheckHandler,
            RequestHandler serviceOneHandler,
            RequestHandler serviceTwoHandler) {

        logger.info("Creating router using OpenAPI specification");

        // Create a map of operation IDs to handlers
        Map<String, RequestHandler> handlers = new HashMap<>();
        handlers.put("getHealth", healthCheckHandler);

        // Service One handlers
        handlers.put("getServiceOneItem", serviceOneHandler);
        handlers.put("createServiceOneItem", serviceOneHandler);
        handlers.put("updateServiceOneItem", serviceOneHandler);
        handlers.put("deleteServiceOneItem", serviceOneHandler);
        handlers.put("listServiceOneItems", serviceOneHandler);

        // Service Two handlers
        handlers.put("getServiceTwoItem", serviceTwoHandler);
        handlers.put("createServiceTwoItem", serviceTwoHandler);

        // Create the router from the OpenAPI specification
        return createRouterFromOpenAPI(handlers);
    }

    /**
     * Creates a router from the OpenAPI specification.
     *
     * @param handlers the map of operation IDs to handlers
     * @return a future with the router
     */
    private Future<Router> createRouterFromOpenAPI(Map<String, RequestHandler> handlers) {
        // Configure RouterBuilder options
        RouterBuilderOptions options = new RouterBuilderOptions()
                .setRequireSecurityHandlers(false)
                .setOperationModelKey("operationModel");

        // Create the RouterBuilder from the OpenAPI specification
        return RouterBuilder.create(vertx, "src/main/resources/openapi.yaml")
                .compose(routerBuilder -> {
                    logger.info("RouterBuilder created successfully");

                    // Configure the RouterBuilder with options
                    routerBuilder.setOptions(options);

                    // Add operation handlers
                    handlers.forEach((operationId, handler) -> {
                        logger.debug("Adding handler for operation: {}", operationId);
                        routerBuilder.operation(operationId).handler(ctx -> handler.handle(ctx));
                    });

                    // Add global handlers
                    routerBuilder.rootHandler(ctx -> {
                        LoggerHandler.create().handle(ctx);
                        ctx.next();
                    });
                    routerBuilder.rootHandler(ctx -> {
                        ResponseTimeHandler.create().handle(ctx);
                        ctx.next();
                    });
                    routerBuilder.rootHandler(ctx -> {
                        BodyHandler.create().handle(ctx);
                        ctx.next();
                    });

                    // Create the router
                    Router router = routerBuilder.createRouter();

                    // Add CORS support
                    configureCors(router);

                    // Add Swagger UI
                    configureSwaggerUI(router);

                    // Fallback handler for 404
                    configureFallbackHandler(router);

                    // Error handler
                    configureErrorHandler(router);

                    logger.info("Router created successfully from OpenAPI specification");
                    return Future.succeededFuture(router);
                })
                .recover(err -> {
                    logger.error("Failed to create router from OpenAPI specification", err);

                    // Fallback to creating a router without OpenAPI
                    logger.info("Creating router without OpenAPI specification");
                    Router router = Router.router(vertx);

                    // Add CORS support
                    configureCors(router);

                    // Add common handlers
                    router.route().handler(LoggerHandler.create());
                    router.route().handler(ResponseTimeHandler.create());
                    router.route().handler(BodyHandler.create());

                    // Health check endpoint
                    router.get("/health").handler(handlers.get("getHealth")::handle);

                    // Service One routes
                    router.get("/api/service-one").handler(handlers.get("listServiceOneItems")::handle);
                    router.post("/api/service-one").handler(handlers.get("createServiceOneItem")::handle);
                    router.get("/api/service-one/:id").handler(handlers.get("getServiceOneItem")::handle);
                    router.put("/api/service-one/:id").handler(handlers.get("updateServiceOneItem")::handle);
                    router.delete("/api/service-one/:id").handler(handlers.get("deleteServiceOneItem")::handle);

                    // Service Two routes
                    router.get("/api/service-two/:id").handler(handlers.get("getServiceTwoItem")::handle);
                    router.post("/api/service-two").handler(handlers.get("createServiceTwoItem")::handle);

                    // Fallback handler for 404
                    configureFallbackHandler(router);

                    // Error handler
                    configureErrorHandler(router);

                    logger.info("Router created successfully without OpenAPI specification");
                    return Future.succeededFuture(router);
                });
    }

    /**
     * Configures Swagger UI.
     *
     * @param router the router
     */
    private void configureSwaggerUI(Router router) {
        logger.debug("Configuring Swagger UI");

        // Serve the OpenAPI specification
        router.get("/openapi.yaml").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "text/yaml")
                .sendFile("openapi.yaml");
        });

        // Serve Swagger UI
        router.get("/swagger-ui/*").handler(StaticHandler.create("META-INF/resources/webjars/swagger-ui/4.18.2")
                .setCachingEnabled(false));

        // Redirect to Swagger UI
        router.get("/swagger-ui").handler(ctx -> {
            ctx.response()
                .putHeader("Location", "/swagger-ui/index.html?url=/openapi.yaml")
                .setStatusCode(302)
                .end();
        });

        // Add a redirect from the root to Swagger UI
        router.get("/docs").handler(ctx -> {
            ctx.response()
                .putHeader("Location", "/swagger-ui")
                .setStatusCode(302)
                .end();
        });

        logger.info("Swagger UI configured successfully");
    }

    /**
     * Configures CORS support.
     *
     * @param router the router
     */
    private void configureCors(Router router) {
        logger.debug("Configuring CORS");

        // Get CORS configuration
        JsonObject corsConfig = config.getJsonObject("cors", new JsonObject());
        boolean corsEnabled = corsConfig.getBoolean("enabled", true);

        if (corsEnabled) {
            Set<String> allowedHeaders = new HashSet<>();
            allowedHeaders.add("x-requested-with");
            allowedHeaders.add("Access-Control-Allow-Origin");
            allowedHeaders.add("origin");
            allowedHeaders.add("Content-Type");
            allowedHeaders.add("accept");
            allowedHeaders.add("Authorization");

            // Add any additional headers from config
            corsConfig.getJsonArray("allowed-headers", new JsonArray())
                    .forEach(header -> allowedHeaders.add(header.toString()));

            Set<HttpMethod> allowedMethods = new HashSet<>();
            allowedMethods.add(HttpMethod.GET);
            allowedMethods.add(HttpMethod.POST);
            allowedMethods.add(HttpMethod.PUT);
            allowedMethods.add(HttpMethod.DELETE);
            allowedMethods.add(HttpMethod.OPTIONS);

            // Add any additional methods from config
            corsConfig.getJsonArray("allowed-methods", new JsonArray())
                    .forEach(method -> allowedMethods.add(HttpMethod.valueOf(method.toString())));

            String allowedOrigin = corsConfig.getString("allowed-origin", "*");

            router.route().handler(CorsHandler.create(allowedOrigin)
                    .allowedHeaders(allowedHeaders)
                    .allowedMethods(allowedMethods));

            logger.info("CORS configured with origin: {}, {} headers, {} methods", 
                    allowedOrigin, allowedHeaders.size(), allowedMethods.size());
        } else {
            logger.info("CORS is disabled");
        }
    }

    /**
     * Configures the fallback handler for 404 errors.
     *
     * @param router the router
     */
    private void configureFallbackHandler(Router router) {
        logger.debug("Configuring fallback handler");

        router.route().last().handler(ctx -> {
            logger.warn("Route not found: {}", ctx.request().uri());
            ctx.response()
                .setStatusCode(404)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("error", "Not Found")
                    .put("message", "Route not found: " + ctx.request().uri())
                    .put("path", ctx.request().uri())
                    .encode());
        });
    }

    /**
     * Configures the error handler.
     *
     * @param router the router
     */
    private void configureErrorHandler(Router router) {
        logger.debug("Configuring error handler");

        router.route().failureHandler(ctx -> {
            int statusCode = ctx.statusCode() > 0 ? ctx.statusCode() : 500;
            Throwable failure = ctx.failure();

            logger.error("Request failed with status code {}: {}", statusCode, 
                    failure != null ? failure.getMessage() : "Unknown error");

            ctx.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("error", statusCode == 404 ? "Not Found" : "Internal Server Error")
                    .put("message", failure != null ? failure.getMessage() : "Unknown error")
                    .put("path", ctx.request().uri())
                    .encode());
        });
    }
}
