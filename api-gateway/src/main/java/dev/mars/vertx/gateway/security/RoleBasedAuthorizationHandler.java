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
 * This handler should be used after a JWT authentication handler.
 */
public class RoleBasedAuthorizationHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAuthorizationHandler.class);

    private final Set<String> requiredRoles;
    private final boolean requireAllRoles;

    /**
     * Creates a new role-based authorization handler that requires any of the specified roles.
     *
     * @param requiredRoles the roles required to access the resource
     */
    public RoleBasedAuthorizationHandler(String... requiredRoles) {
        this(false, requiredRoles);
    }

    /**
     * Creates a new role-based authorization handler.
     *
     * @param requireAllRoles if true, the user must have all required roles; if false, any one role is sufficient
     * @param requiredRoles the roles required to access the resource
     */
    public RoleBasedAuthorizationHandler(boolean requireAllRoles, String... requiredRoles) {
        this.requiredRoles = new HashSet<>(Arrays.asList(requiredRoles));
        this.requireAllRoles = requireAllRoles;

        logger.info("Created role-based authorization handler with required roles: {}, requireAllRoles: {}", 
                this.requiredRoles, this.requireAllRoles);
    }

    @Override
    public void handle(RoutingContext context) {
        logger.debug("Handling role-based authorization for request: {}", context.request().uri());

        // Get the user from the context
        JsonObject user = context.user() != null ? context.user().principal() : null;

        if (user == null) {
            logger.warn("No authenticated user found for request: {}", context.request().uri());
            sendUnauthorizedResponse(context);
            return;
        }

        // Get the user roles
        String rolesStr = user.getString("roles", "");
        Set<String> userRoles = new HashSet<>();

        // Handle empty roles string
        if (!rolesStr.isEmpty()) {
            userRoles.addAll(Arrays.asList(rolesStr.split(",")));
        }

        // Check if the user has the required roles
        boolean authorized;

        if (requireAllRoles) {
            // User must have all required roles
            authorized = userRoles.containsAll(requiredRoles);
        } else {
            // User must have at least one of the required roles
            authorized = false;
            for (String role : requiredRoles) {
                if (userRoles.contains(role)) {
                    authorized = true;
                    break;
                }
            }
        }

        if (authorized) {
            logger.debug("User {} has required role(s) for request: {}", user.getString("sub"), context.request().uri());
            context.next();
        } else {
            logger.warn("User {} does not have required role(s) for request: {}", user.getString("sub"), context.request().uri());
            sendForbiddenResponse(context);
        }
    }

    /**
     * Sends an unauthorized (401) response.
     *
     * @param context the routing context
     */
    private void sendUnauthorizedResponse(RoutingContext context) {
        context.response()
            .setStatusCode(401)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("error", "Unauthorized")
                .put("message", "Authentication required")
                .put("path", context.request().uri())
                .encode());
    }

    /**
     * Sends a forbidden (403) response.
     *
     * @param context the routing context
     */
    private void sendForbiddenResponse(RoutingContext context) {
        context.response()
            .setStatusCode(403)
            .putHeader("Content-Type", "application/json")
            .end(new JsonObject()
                .put("error", "Forbidden")
                .put("message", "Insufficient permissions")
                .put("path", context.request().uri())
                .encode());
    }

    /**
     * Gets the required roles.
     *
     * @return the required roles
     */
    public Set<String> getRequiredRoles() {
        return requiredRoles;
    }

    /**
     * Checks if all roles are required.
     *
     * @return true if all roles are required, false if any one role is sufficient
     */
    public boolean isRequireAllRoles() {
        return requireAllRoles;
    }
}
