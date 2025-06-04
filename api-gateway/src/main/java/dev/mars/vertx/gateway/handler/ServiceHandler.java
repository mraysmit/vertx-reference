package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Generic handler for service requests.
 * Uses a request transformer function to customize the request object.
 */
public class ServiceHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHandler.class);
    
    private final MicroserviceClient client;
    private final Function<RoutingContext, JsonObject> requestTransformer;
    private final String serviceName;
    
    /**
     * Creates a new service handler.
     *
     * @param client the microservice client
     * @param requestTransformer the function to transform the routing context into a request object
     * @param serviceName the name of the service for logging
     */
    public ServiceHandler(MicroserviceClient client, Function<RoutingContext, JsonObject> requestTransformer, String serviceName) {
        this.client = client;
        this.requestTransformer = requestTransformer;
        this.serviceName = serviceName;
    }
    
    /**
     * Creates a new service handler with a default request transformer.
     *
     * @param client the microservice client
     * @param serviceName the name of the service for logging
     */
    public ServiceHandler(MicroserviceClient client, String serviceName) {
        this(client, ServiceHandler::createDefaultRequestObject, serviceName);
    }
    
    @Override
    public void handle(RoutingContext context) {
        try {
            logger.debug("Handling {} service request: {}", serviceName, context.request().uri());
            
            // Create request object using the transformer
            JsonObject request = requestTransformer.apply(context);
            
            // Send request to service
            client.sendRequest(request)
                .onSuccess(response -> {
                    logger.debug("Received response from {} service: {}", serviceName, response);
                    sendResponse(context, response);
                })
                .onFailure(err -> {
                    logger.error("Error calling {} service", serviceName, err);
                    handleError(context, new RuntimeException("Service unavailable: " + err.getMessage()));
                });
        } catch (Exception e) {
            handleError(context, e);
        }
    }
    
    /**
     * Creates a default request object from the routing context.
     *
     * @param context the routing context
     * @return the request object
     */
    public static JsonObject createDefaultRequestObject(RoutingContext context) {
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
    
    /**
     * Handles an error.
     *
     * @param context the routing context
     * @param e the exception
     */
    protected void handleError(RoutingContext context, Throwable e) {
        logger.error("Error handling request: {}", e.getMessage(), e);
        
        int statusCode = 500;
        String errorMessage = e.getMessage();
        
        // Determine appropriate status code based on exception type
        if (e instanceof IllegalArgumentException) {
            statusCode = 400; // Bad Request
        }
        
        sendError(context, statusCode, errorMessage);
    }
    
    /**
     * Sends an error response.
     *
     * @param context the routing context
     * @param statusCode the HTTP status code
     * @param message the error message
     */
    protected void sendError(RoutingContext context, int statusCode, String message) {
        JsonObject response = new JsonObject()
                .put("error", statusCode == 404 ? "Not Found" : "Internal Server Error")
                .put("message", message)
                .put("path", context.request().uri());
        
        context.response()
                .setStatusCode(statusCode)
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }
    
    /**
     * Sends a JSON response.
     *
     * @param context the routing context
     * @param response the response object
     */
    protected void sendResponse(RoutingContext context, JsonObject response) {
        context.response()
                .putHeader("Content-Type", "application/json")
                .end(response.encode());
    }
}