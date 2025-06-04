package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.function.Function;

/**
 * Handler for Service Two requests.
 * Handles requests to the Service Two microservice.
 *
 * This handler provides factory methods to create specialized handlers for different
 * Service Two actions, using a more flexible approach with request transformers.
 */
public class ServiceTwoHandler extends ServiceRequestHandler {

    /**
     * Creates a new Service Two handler.
     *
     * @param serviceClient the client for Service Two
     */
    public ServiceTwoHandler(MicroserviceClient serviceClient) {
        super(serviceClient, "service-two");
    }

    @Override
    protected void handleRequest(RoutingContext context) {
        logger.debug("Handling Service Two request: {}", context.request().uri());

        // Call the parent handler
        super.handleRequest(context);
    }

    /**
     * Creates a handler for cities requests.
     *
     * @param serviceClient the client for Service Two
     * @return a handler for cities requests
     */
    public static Handler<RoutingContext> createCitiesHandler(MicroserviceClient serviceClient) {
        return new ServiceHandler(serviceClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", "cities");
            return request;
        }, "service-two");
    }

    /**
     * Creates a handler for forecast requests.
     *
     * @param serviceClient the client for Service Two
     * @return a handler for forecast requests
     */
    public static Handler<RoutingContext> createForecastHandler(MicroserviceClient serviceClient) {
        return new ServiceHandler(serviceClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", "forecast");
            return request;
        }, "service-two");
    }

    /**
     * Creates a handler for random weather requests.
     *
     * @param serviceClient the client for Service Two
     * @return a handler for random weather requests
     */
    public static Handler<RoutingContext> createRandomWeatherHandler(MicroserviceClient serviceClient) {
        return new ServiceHandler(serviceClient, "service-two");
    }

    /**
     * Creates a handler for stats requests.
     *
     * @param serviceClient the client for Service Two
     * @return a handler for stats requests
     */
    public static Handler<RoutingContext> createStatsHandler(MicroserviceClient serviceClient) {
        return new ServiceHandler(serviceClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", "stats");
            return request;
        }, "service-two");
    }

    /**
     * Creates a handler for weather item requests.
     *
     * @param serviceClient the client for Service Two
     * @return a handler for weather item requests
     */
    public static Handler<RoutingContext> createWeatherItemHandler(MicroserviceClient serviceClient) {
        return new ServiceHandler(serviceClient, "service-two");
    }

    /**
     * Creates a handler for a custom action.
     *
     * @param serviceClient the client for Service Two
     * @param action the action to set in the request
     * @return a handler for the specified action
     */
    public static Handler<RoutingContext> createActionHandler(MicroserviceClient serviceClient, String action) {
        return new ServiceHandler(serviceClient, ctx -> {
            JsonObject request = ServiceHandler.createDefaultRequestObject(ctx);
            request.put("action", action);
            return request;
        }, "service-two");
    }
}
