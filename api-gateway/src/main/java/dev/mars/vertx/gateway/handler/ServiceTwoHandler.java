package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.ext.web.RoutingContext;

/**
 * Handler for Service Two requests.
 * Handles requests to the Service Two microservice.
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
        
        // Add any Service Two specific handling here
        
        // Call the parent handler
        super.handleRequest(context);
    }
}