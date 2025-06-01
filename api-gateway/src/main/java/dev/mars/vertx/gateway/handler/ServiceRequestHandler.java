package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Base handler for service requests.
 * Handles requests to microservices via the event bus.
 */
public class ServiceRequestHandler extends AbstractRequestHandler {

    private final MicroserviceClient serviceClient;
    private final String serviceName;

    /**
     * Creates a new service request handler.
     *
     * @param serviceClient the client for the service
     * @param serviceName the name of the service
     */
    public ServiceRequestHandler(MicroserviceClient serviceClient, String serviceName) {
        this.serviceClient = serviceClient;
        this.serviceName = serviceName;
    }

    @Override
    protected void handleRequest(RoutingContext context) {
        logger.debug("Handling {} service request: {}", serviceName, context.request().uri());

        // Create request object
        JsonObject request = createRequestObject(context);

        // Send request to service
        serviceClient.sendRequest(request)
            .onSuccess(response -> {
                logger.debug("Received response from {} service: {}", serviceName, response);
                sendResponse(context, response);
            })
            .onFailure(err -> {
                logger.error("Error calling {} service", serviceName, err);
                handleError(context, new RuntimeException("Service unavailable: " + err.getMessage()));
            });
    }

    /**
     * Creates a request object from the routing context.
     *
     * @param context the routing context
     * @return the request object
     */
    protected JsonObject createRequestObject(RoutingContext context) {
        JsonObject request = new JsonObject();

        // Add path parameters
        context.pathParams().forEach(request::put);

        // Add query parameters
        context.queryParams().forEach(entry -> request.put(entry.getKey(), entry.getValue()));

        // Add body if present
        if (context.getBody() != null && context.getBody().length() > 0) {
            try {
                JsonObject body = context.getBodyAsJson();
                request.mergeIn(body);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid JSON body");
            }
        }

        return request;
    }
}
