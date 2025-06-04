package dev.mars.vertx.gateway.security;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the JwtAuthHandler class.
 * Tests JWT token validation and generation.
 */
@ExtendWith(VertxExtension.class)
class JwtAuthHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthHandlerTest.class);
    private static final int TEST_PORT = 9998;
    private static final String TEST_HOST = "localhost";
    private static final String TEST_SECRET_KEY = "test-jwt-secret-key-for-authentication";

    private Vertx vertx;
    private HttpClient client;
    private HttpServer server;
    private JWTAuth jwtAuth;
    private JwtAuthHandler jwtAuthHandler;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Create HTTP client
        client = vertx.createHttpClient();

        // Create JWT auth provider with proper configuration
        JWTAuthOptions options = new JWTAuthOptions();
        options.addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("HS256")
                .setSymmetric(true)
                .setBuffer(TEST_SECRET_KEY));
        jwtAuth = JWTAuth.create(vertx, options);

        // Create JWT auth handler
        jwtAuthHandler = new JwtAuthHandler(jwtAuth, "test-realm", 3600);

        // Create a router with secured endpoints
        Router router = Router.router(vertx);

        // Public endpoint - no authentication required
        router.get("/api/public").handler(this::handlePublicEndpoint);

        // Secured endpoint - authentication required
        router.get("/api/secured")
                .handler(jwtAuthHandler)
                .handler(this::handleSecuredEndpoint);

        // Start a test HTTP server
        server = vertx.createHttpServer();
        server.requestHandler(router)
                .listen(TEST_PORT)
                .onComplete(testContext.succeeding(httpServer -> {
                    logger.info("Test server started on port {}", TEST_PORT);
                    testContext.completeNow();
                }));
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        client.close();
        if (server != null) {
            server.close()
                    .onComplete(testContext.succeeding(v -> {
                        vertx.close()
                                .onComplete(testContext.succeeding(v2 -> {
                                    testContext.completeNow();
                                }));
                    }));
        } else {
            vertx.close()
                    .onComplete(testContext.succeeding(v -> {
                        testContext.completeNow();
                    }));
        }
    }

    @Test
    void testPublicEndpoint(VertxTestContext testContext) {
        // Make a request to the public endpoint without authentication
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/public")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Parse the response body as JSON
                    JsonObject json = new JsonObject(body);

                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals("public", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testSecuredEndpointWithoutAuthentication(VertxTestContext testContext) {
        // Make a request to the secured endpoint without authentication
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/secured")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(401, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // The default Vert.x JWT auth handler returns a plain text "Unauthorized" response
                    // rather than a JSON response
                    String responseText = body.toString();

                    // Verify the response content
                    testContext.verify(() -> {
                        assertTrue(responseText.contains("Unauthorized"), 
                                "Response should contain 'Unauthorized' but was: " + responseText);
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testSecuredEndpointWithAuthentication(VertxTestContext testContext) {
        // Generate a token for a test user
        String token = jwtAuthHandler.generateToken("test-user", new String[]{"user"});

        // Make a request to the secured endpoint with authentication
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/secured")
                .compose(request -> {
                    request.putHeader("Authorization", "Bearer " + token);
                    return request.send();
                })
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Parse the response body as JSON
                    JsonObject json = new JsonObject(body);

                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals("secured", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("test-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testTokenGeneration() {
        // Generate a token for a test user
        String token = jwtAuthHandler.generateToken("test-user", new String[]{"user", "admin"});

        // Verify the token is not null
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // Verify the token can be decoded
        jwtAuth.authenticate(new JsonObject().put("token", token))
                .onSuccess(user -> {
                    // Verify the user principal
                    JsonObject principal = user.principal();
                    assertEquals("test-user", principal.getString("sub"));
                    assertEquals("user,admin", principal.getString("roles"));
                })
                .onFailure(err -> {
                    fail("Failed to authenticate token: " + err.getMessage());
                });
    }

    @Test
    void testGetters() {
        // Test the getters
        assertEquals(jwtAuth, jwtAuthHandler.getJwtAuth());
        assertEquals("test-realm", jwtAuthHandler.getRealm());
        assertEquals(3600, jwtAuthHandler.getTokenExpirationSeconds());
    }

    // Handler for public endpoint
    private void handlePublicEndpoint(RoutingContext context) {
        context.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("endpoint", "public")
                        .put("status", "success")
                        .encode());
    }

    // Handler for secured endpoint
    private void handleSecuredEndpoint(RoutingContext context) {
        String userId = context.user().principal().getString("sub");

        context.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("endpoint", "secured")
                        .put("status", "success")
                        .put("user", userId)
                        .encode());
    }
}
