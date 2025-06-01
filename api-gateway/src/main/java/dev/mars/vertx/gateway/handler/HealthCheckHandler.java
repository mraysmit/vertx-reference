package dev.mars.vertx.gateway.handler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for health check requests.
 * Returns the status of the API Gateway.
 */
public class HealthCheckHandler extends AbstractRequestHandler {
    
    @Override
    protected void handleRequest(RoutingContext context) {
        logger.debug("Handling health check request");
        
        JsonObject health = new JsonObject()
                .put("status", "UP")
                .put("timestamp", System.currentTimeMillis());
        
        sendResponse(context, health);
    }
}