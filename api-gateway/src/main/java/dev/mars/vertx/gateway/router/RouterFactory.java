package dev.mars.vertx.gateway.router;

import dev.mars.vertx.gateway.handler.ServiceHandler;
import io.vertx.core.Handler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Simplified factory for creating and configuring routers.
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
     * @param handlers the map of path patterns to handlers
     * @return the router
     */
    public Future<Router> createRouter(Map<String, io.vertx.core.Handler<io.vertx.ext.web.RoutingContext>> handlers) {
        logger.info("Creating router with {} handlers", handlers.size());
        Router router = Router.router(vertx);

        // Add common middleware
        router.route().handler(LoggerHandler.create());
        router.route().handler(ResponseTimeHandler.create());
        router.route().handler(BodyHandler.create());

        // Configure CORS if enabled
        configureCors(router);

        // Configure routes from handlers map
        configureRoutes(router, handlers);

        // Add error handling
        configureErrorHandler(router);

        logger.info("Router created successfully");
        return Future.succeededFuture(router);
    }

    /**
     * Configures routes from the handlers map.
     *
     * @param router the router
     * @param handlers the map of path patterns to handlers
     */
    private void configureRoutes(Router router, Map<String, io.vertx.core.Handler<io.vertx.ext.web.RoutingContext>> handlers) {
        handlers.forEach((path, handler) -> {
            if (path.startsWith("GET:")) {
                String routePath = path.substring(4);
                router.get(routePath).handler(handler);
                logger.debug("Added GET route: {}", routePath);
            } else if (path.startsWith("POST:")) {
                String routePath = path.substring(5);
                router.post(routePath).handler(handler);
                logger.debug("Added POST route: {}", routePath);
            } else if (path.startsWith("PUT:")) {
                String routePath = path.substring(4);
                router.put(routePath).handler(handler);
                logger.debug("Added PUT route: {}", routePath);
            } else if (path.startsWith("DELETE:")) {
                String routePath = path.substring(7);
                router.delete(routePath).handler(handler);
                logger.debug("Added DELETE route: {}", routePath);
            } else {
                // Default to GET if no method specified
                router.get(path).handler(handler);
                logger.debug("Added default GET route: {}", path);
            }
        });

        // Add fallback handler for 404
        configureFallbackHandler(router);
    }

    /**
     * Configures CORS support.
     *
     * @param router the router
     */
    private void configureCors(Router router) {
        // Get CORS configuration
        JsonObject corsConfig = config.getJsonObject("cors", new JsonObject());
        boolean corsEnabled = corsConfig.getBoolean("enabled", true);

        if (corsEnabled) {
            logger.debug("Configuring CORS");

            // Create a set of allowed headers with sensible defaults
            Set<String> allowedHeaders = new HashSet<>();
            allowedHeaders.add("x-requested-with");
            allowedHeaders.add("Access-Control-Allow-Origin");
            allowedHeaders.add("origin");
            allowedHeaders.add("Content-Type");
            allowedHeaders.add("accept");
            allowedHeaders.add("Authorization");

            // Add custom headers from configuration if provided
            JsonArray customHeaders = corsConfig.getJsonArray("allowed-headers");
            if (customHeaders != null) {
                for (int i = 0; i < customHeaders.size(); i++) {
                    allowedHeaders.add(customHeaders.getString(i));
                }
                logger.debug("Added custom allowed headers: {}", customHeaders);
            }

            // Create a set of allowed methods with sensible defaults
            Set<HttpMethod> allowedMethods = new HashSet<>();
            allowedMethods.add(HttpMethod.GET);
            allowedMethods.add(HttpMethod.POST);
            allowedMethods.add(HttpMethod.PUT);
            allowedMethods.add(HttpMethod.DELETE);
            allowedMethods.add(HttpMethod.OPTIONS);

            // Add custom methods from configuration if provided
            JsonArray customMethods = corsConfig.getJsonArray("allowed-methods");
            if (customMethods != null) {
                for (int i = 0; i < customMethods.size(); i++) {
                    String methodName = customMethods.getString(i);
                    try {
                        HttpMethod method = HttpMethod.valueOf(methodName);
                        allowedMethods.add(method);
                        logger.debug("Added custom allowed method: {}", methodName);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Invalid HTTP method in CORS configuration: {}", methodName);
                    }
                }
            }

            // Use configured allowed origin or default to "*"
            String allowedOrigin = corsConfig.getString("allowed-origin", "*");
            logger.debug("Using allowed origin: {}", allowedOrigin);

            // Add CORS handler to all routes
            router.route().handler(CorsHandler.create(allowedOrigin)
                    .allowedHeaders(allowedHeaders)
                    .allowedMethods(allowedMethods));

            logger.info("CORS configured with custom settings");
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
