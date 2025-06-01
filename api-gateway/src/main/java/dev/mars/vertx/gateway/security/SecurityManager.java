package dev.mars.vertx.gateway.security;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for security-related functionality.
 * Initializes and manages JWT authentication provider.
 */
public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);

    private final Vertx vertx;
    private final JsonObject config;
    private final JWTAuth jwtAuth;
    private final JwtAuthHandler jwtAuthHandler;
    private final boolean securityEnabled;

    /**
     * Creates a new security manager.
     *
     * @param vertx the Vertx instance
     * @param config the configuration
     */
    public SecurityManager(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;

        // Check if security is enabled
        this.securityEnabled = config.getJsonObject("security", new JsonObject())
                .getBoolean("enabled", false);

        if (securityEnabled) {
            logger.info("Initializing security manager with security enabled");

            // Get JWT configuration
            JsonObject jwtConfig = config.getJsonObject("security", new JsonObject())
                    .getJsonObject("jwt", new JsonObject());

            // Create JWT auth options
            JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();

            // Check if we should use a keystore or a symmetric key
            if (jwtConfig.containsKey("keystore")) {
                // Get keystore configuration
                JsonObject keystoreConfig = jwtConfig.getJsonObject("keystore", new JsonObject());
                String keystorePath = keystoreConfig.getString("path", "keystore.jceks");
                String keystoreType = keystoreConfig.getString("type", "jceks");
                String keystorePassword = keystoreConfig.getString("password", "secret");

                // Create keystore options
                KeyStoreOptions keyStoreOptions = new KeyStoreOptions()
                        .setPath(keystorePath)
                        .setType(keystoreType)
                        .setPassword(keystorePassword);

                // Set keystore options
                jwtAuthOptions.setKeyStore(keyStoreOptions);

                logger.info("Using keystore for JWT authentication: {}", keystorePath);
            } else if (jwtConfig.containsKey("symmetric-key")) {
                // Get symmetric key
                String symmetricKey = jwtConfig.getString("symmetric-key", "super-secret-key");

                // Set symmetric key
                PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setSymmetric(true)
                        .setSecretKey(symmetricKey);

                jwtAuthOptions.addPubSecKey(pubSecKeyOptions);

                logger.info("Using symmetric key for JWT authentication");
            } else {
                // Default to a test symmetric key
                String defaultKey = "default-test-key-for-jwt-authentication";

                // Set default symmetric key
                PubSecKeyOptions defaultPubSecKeyOptions = new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setSymmetric(true)
                        .setSecretKey(defaultKey);

                jwtAuthOptions.addPubSecKey(defaultPubSecKeyOptions);

                logger.info("Using default symmetric key for JWT authentication");
            }

            // Create JWT auth provider
            this.jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

            // Create JWT auth handler
            this.jwtAuthHandler = new JwtAuthHandler(jwtAuth, "api-gateway");

            logger.info("Security manager initialized with JWT authentication");
        } else {
            logger.info("Initializing security manager with security disabled");
            this.jwtAuth = null;
            this.jwtAuthHandler = null;
        }
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
     * Gets the JWT authentication provider.
     *
     * @return the JWT authentication provider, or null if security is disabled
     */
    public JWTAuth getJwtAuth() {
        return jwtAuth;
    }

    /**
     * Gets the JWT authentication handler.
     *
     * @return the JWT authentication handler, or null if security is disabled
     */
    public JwtAuthHandler getJwtAuthHandler() {
        return jwtAuthHandler;
    }

    /**
     * Creates a role-based authorization handler.
     *
     * @param requiredRoles the roles required to access the resource
     * @return the role-based authorization handler
     */
    public RoleBasedAuthorizationHandler createRoleBasedAuthHandler(String... requiredRoles) {
        return new RoleBasedAuthorizationHandler(requiredRoles);
    }

    /**
     * Generates a JWT token for the specified user.
     *
     * @param userId the user ID
     * @param roles the user roles
     * @return the JWT token, or null if security is disabled
     */
    public String generateToken(String userId, String[] roles) {
        if (!securityEnabled || jwtAuthHandler == null) {
            logger.warn("Cannot generate token: security is disabled");
            return null;
        }

        return jwtAuthHandler.generateToken(userId, roles);
    }
}
