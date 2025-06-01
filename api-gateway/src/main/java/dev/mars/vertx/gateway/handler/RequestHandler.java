package dev.mars.vertx.gateway.handler;

import io.vertx.ext.web.RoutingContext;

/**
 * Interface for request handlers.
 * Defines a common contract for all handlers.
 */
public interface RequestHandler {
    
    /**
     * Handles a request.
     *
     * @param context the routing context
     */
    void handle(RoutingContext context);
}