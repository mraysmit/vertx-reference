package dev.mars.vertx.gateway.security;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for JWT authentication.
 * Validates JWT tokens in the Authorization header.
 * This is a simplified wrapper around Vert.x's JWTAuthHandler.
 */
public class JwtAuthHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthHandler.class);

    private final JWTAuth jwtAuth;
    private final Handler<RoutingContext> jwtAuthHandler;
    private final String realm;
    private final int tokenExpirationSeconds;

    /**
     * Creates a new JWT authentication handler with default settings.
     *
     * @param jwtAuth the JWT authentication provider
     */
    public JwtAuthHandler(JWTAuth jwtAuth) {
        this(jwtAuth, "api-gateway", 3600);
    }

    /**
     * Creates a new JWT authentication handler.
     *
     * @param jwtAuth the JWT authentication provider
     * @param realm the authentication realm
     */
    public JwtAuthHandler(JWTAuth jwtAuth, String realm) {
        this(jwtAuth, realm, 3600);
    }

    /**
     * Creates a new JWT authentication handler with custom settings.
     *
     * @param jwtAuth the JWT authentication provider
     * @param realm the authentication realm
     * @param tokenExpirationSeconds the token expiration time in seconds
     */
    public JwtAuthHandler(JWTAuth jwtAuth, String realm, int tokenExpirationSeconds) {
        this.jwtAuth = jwtAuth;
        this.realm = realm;
        this.tokenExpirationSeconds = tokenExpirationSeconds;

        // Create the Vert.x JWT auth handler
        this.jwtAuthHandler = JWTAuthHandler.create(jwtAuth);

        logger.info("Created JWT authentication handler for realm: {}", realm);
    }

    @Override
    public void handle(RoutingContext context) {
        logger.debug("Handling JWT authentication for request: {}", context.request().uri());

        // Let the Vert.x JWT auth handler do the work
        jwtAuthHandler.handle(context);
    }

    /**
     * Generates a JWT token for the specified user.
     *
     * @param userId the user ID
     * @param roles the user roles
     * @return the JWT token
     */
    public String generateToken(String userId, String[] roles) {
        logger.debug("Generating JWT token for user: {}", userId);

        // Create claims
        JsonObject claims = new JsonObject()
                .put("sub", userId)
                .put("roles", String.join(",", roles))
                .put("iat", System.currentTimeMillis() / 1000)
                .put("exp", System.currentTimeMillis() / 1000 + tokenExpirationSeconds);

        // Generate token
        JWTOptions options = new JWTOptions()
                .setExpiresInSeconds(tokenExpirationSeconds)
                .setIssuer(realm)
                .setSubject(userId);

        return jwtAuth.generateToken(claims, options);
    }

    /**
     * Gets the JWT authentication provider.
     *
     * @return the JWT authentication provider
     */
    public JWTAuth getJwtAuth() {
        return jwtAuth;
    }

    /**
     * Gets the authentication realm.
     *
     * @return the authentication realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Gets the token expiration time in seconds.
     *
     * @return the token expiration time in seconds
     */
    public int getTokenExpirationSeconds() {
        return tokenExpirationSeconds;
    }
}
