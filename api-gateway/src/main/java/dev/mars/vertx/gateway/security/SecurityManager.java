package dev.mars.vertx.gateway.security;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simplified manager for security-related functionality.
 * Security is optional and disabled by default.
 */
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private final Vertx vertx;
    private final boolean securityEnabled;
    private final JWTAuth jwtAuth;
    private final Handler<RoutingContext> jwtAuthHandler;
    private final String realm;

    /**
     * Creates a new security manager.
     *
     * @param vertx the Vertx instance
     * @param config the configuration
     */
    public SecurityManager(Vertx vertx, JsonObject config) {
        this.vertx = vertx;

        // Check if security is enabled (disabled by default)
        JsonObject securityConfig = config.getJsonObject("security", new JsonObject());
        this.securityEnabled = securityConfig.getBoolean("enabled", false);
        this.realm = securityConfig.getString("realm", "api-gateway");

        // Initialize JWT auth and handler to null by default
        JWTAuth jwtAuthTemp = null;
        Handler<RoutingContext> jwtAuthHandlerTemp = null;

        if (securityEnabled) {
            logger.info("Initializing security manager with security enabled");

            try {
                // Create JWT auth with sensible defaults
                // Look for JWT configuration in different possible locations for backward compatibility
                JsonObject jwtConfig = securityConfig.getJsonObject("jwt", new JsonObject());

                // Try to get the secret key from different possible locations
                String secretKey = securityConfig.getString("secret-key", null);
                if (secretKey == null) {
                    secretKey = jwtConfig.getString("symmetric-key", "default-api-gateway-secret-key");
                }

                // Ensure the secret key is not null or empty
                if (secretKey == null || secretKey.isEmpty()) {
                    secretKey = "default-api-gateway-secret-key";
                }

                logger.info("Using symmetric key for JWT authentication: {}", secretKey);

                // Create JWT auth options with the symmetric key
                JWTAuthOptions options = new JWTAuthOptions();

                // Add the public/secret key options
                PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions()
                    .setAlgorithm("HS256")
                    .setSymmetric(true)
                    .setBuffer(secretKey);  // Use buffer for symmetric keys

                options.addPubSecKey(pubSecKeyOptions);

                jwtAuthTemp = JWTAuth.create(vertx, options);
                jwtAuthHandlerTemp = JWTAuthHandler.create(jwtAuthTemp);

                logger.info("Security manager initialized with JWT authentication");
            } catch (Exception e) {
                // If JWT initialization fails, log the error and disable security
                logger.error("Failed to initialize JWT authentication: {}", e.getMessage());
                logger.info("Falling back to disabled security");
                // jwtAuthTemp and jwtAuthHandlerTemp remain null
            }
        } else {
            logger.info("Security is disabled");
            // jwtAuthTemp and jwtAuthHandlerTemp remain null
        }

        // Assign the final values to the instance variables
        this.jwtAuth = jwtAuthTemp;
        this.jwtAuthHandler = jwtAuthHandlerTemp;
    }

    /**
     * Checks if security is enabled.
     *
     * @return true if security is enabled, false otherwise
     */
    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    /**
     * Gets the JWT authentication handler.
     *
     * @return the JWT authentication handler, or null if security is disabled
     */
    public Handler<RoutingContext> getJwtAuthHandler() {
        return jwtAuthHandler;
    }

    /**
     * Creates a role-based authorization handler.
     *
     * @param requiredRoles the roles required to access the resource
     * @return the role-based authorization handler, or null if security is disabled
     */
    public Handler<RoutingContext> createRoleBasedAuthHandler(String... requiredRoles) {
        if (!securityEnabled) {
            logger.warn("Cannot create role-based auth handler: security is disabled");
            return null;
        }

        return new RoleBasedAuthorizationHandler(requiredRoles);
    }

    /**
     * Generates a JWT token for the specified user.
     *
     * @param userId the user ID
     * @param roles the user roles array
     * @return the JWT token, or null if security is disabled
     */
    public String generateToken(String userId, String[] roles) {
        if (!securityEnabled || jwtAuth == null) {
            logger.warn("Cannot generate token: security is disabled");
            return null;
        }

        // Convert roles array to comma-separated string
        String rolesStr = String.join(",", roles);

        JsonObject claims = new JsonObject()
                .put("sub", userId)
                .put("roles", rolesStr)
                .put("iat", System.currentTimeMillis() / 1000)
                .put("exp", System.currentTimeMillis() / 1000 + 3600); // 1 hour expiration

        return jwtAuth.generateToken(claims);
    }
}
