package dev.mars.vertx.gateway.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for request handlers.
 * Provides common functionality for all handlers.
 */
public abstract class AbstractRequestHandler implements RequestHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public void handle(RoutingContext context) {
        try {
            handleRequest(context);
        } catch (Exception e) {
            handleError(context, e);
        }
    }
    
    /**
     * Handles the request.
     * To be implemented by subclasses.
     *
     * @param context the routing context
     */
    protected abstract void handleRequest(RoutingContext context);
    
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