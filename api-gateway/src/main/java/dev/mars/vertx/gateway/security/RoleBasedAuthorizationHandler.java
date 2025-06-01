package dev.mars.vertx.gateway.security;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Handler for role-based authorization.
 * Checks if the authenticated user has the required roles.
 */
public class RoleBasedAuthorizationHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAuthorizationHandler.class);
    
    private final Set<String> requiredRoles;
    
    /**
     * Creates a new role-based authorization handler.
     *
     * @param requiredRoles the roles required to access the resource
     */
    public RoleBasedAuthorizationHandler(String... requiredRoles) {
        this.requiredRoles = new HashSet<>(Arrays.asList(requiredRoles));
        logger.info("Created role-based authorization handler with required roles: {}", this.requiredRoles);
    }
    
    @Override
    public void handle(RoutingContext context) {
        logger.debug("Handling role-based authorization for request: {}", context.request().uri());
        
        // Get the user from the context
        JsonObject user = context.user() != null ? context.user().principal() : null;
        
        if (user == null) {
            logger.warn("No authenticated user found for request: {}", context.request().uri());
            context.response()
                .setStatusCode(401)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("error", "Unauthorized")
                    .put("message", "Authentication required")
                    .put("path", context.request().uri())
                    .encode());
            return;
        }
        
        // Get the user roles
        String rolesStr = user.getString("roles", "");
        Set<String> userRoles = new HashSet<>(Arrays.asList(rolesStr.split(",")));
        
        // Check if the user has any of the required roles
        boolean hasRequiredRole = false;
        for (String role : requiredRoles) {
            if (userRoles.contains(role)) {
                hasRequiredRole = true;
                break;
            }
        }
        
        if (hasRequiredRole) {
            logger.debug("User {} has required role(s) for request: {}", user.getString("sub"), context.request().uri());
            context.next();
        } else {
            logger.warn("User {} does not have required role(s) for request: {}", user.getString("sub"), context.request().uri());
            context.response()
                .setStatusCode(403)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("error", "Forbidden")
                    .put("message", "Insufficient permissions")
                    .put("path", context.request().uri())
                    .encode());
        }
    }
}