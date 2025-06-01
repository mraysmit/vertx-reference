package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for Service One requests.
 * Handles requests to the Service One microservice.
 */
public class ServiceOneHandler extends ServiceRequestHandler {
    
    /**
     * Creates a new Service One handler.
     *
     * @param serviceClient the client for Service One
     */
    public ServiceOneHandler(MicroserviceClient serviceClient) {
        super(serviceClient, "service-one");
    }
    
    @Override
    protected void handleRequest(RoutingContext context) {
        logger.debug("Handling Service One request: {}", context.request().uri());
        
        // Add any Service One specific handling here
        
        // Call the parent handler
        super.handleRequest(context);
    }
}