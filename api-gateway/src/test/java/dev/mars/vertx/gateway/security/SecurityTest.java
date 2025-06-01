package dev.mars.vertx.gateway.security;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
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
 * Tests for the security package.
 * Tests JWT validation, role-based access control, and rejection of unauthorized requests.
 */
@ExtendWith(VertxExtension.class)
class SecurityTest {

    private static final Logger logger = LoggerFactory.getLogger(SecurityTest.class);
    private static final int TEST_PORT = 9999;
    private static final String TEST_HOST = "localhost";
    
    private Vertx vertx;
    private HttpClient client;
    private SecurityManager securityManager;
    
    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        
        // Configure HTTP client
        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(TEST_HOST)
                .setDefaultPort(TEST_PORT);
        
        client = vertx.createHttpClient(options);
        
        // Create security configuration with security enabled and a test symmetric key
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true)
                        .put("jwt", new JsonObject()
                                .put("symmetric-key", "test-symmetric-key-for-jwt-authentication")));
        
        // Create security manager
        securityManager = new SecurityManager(vertx, config);
        
        // Create a router with secured endpoints
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        
        // Public endpoint - no authentication required
        router.get("/api/public").handler(this::handlePublicEndpoint);
        
        // Secured endpoint - authentication required
        router.get("/api/secured").handler(securityManager.getJwtAuthHandler()).handler(this::handleSecuredEndpoint);
        
        // Admin endpoint - authentication and admin role required
        router.get("/api/admin")
                .handler(securityManager.getJwtAuthHandler())
                .handler(securityManager.createRoleBasedAuthHandler("admin"))
                .handler(this::handleAdminEndpoint);
        
        // User endpoint - authentication and user role required
        router.get("/api/user")
                .handler(securityManager.getJwtAuthHandler())
                .handler(securityManager.createRoleBasedAuthHandler("user"))
                .handler(this::handleUserEndpoint);
        
        // Start a test HTTP server
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(TEST_PORT)
                .onComplete(testContext.succeeding(server -> {
                    logger.info("Test server started on port {}", TEST_PORT);
                    testContext.completeNow();
                }));
    }
    
    @AfterEach
    void tearDown(VertxTestContext testContext) {
        client.close();
        vertx.close()
                .onComplete(testContext.succeeding(v -> {
                    testContext.completeNow();
                }));
    }
    
    @Test
    void testPublicEndpoint(VertxTestContext testContext) {
        // Make a request to the public endpoint without authentication
        client.request(HttpMethod.GET, "/api/public")
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
        client.request(HttpMethod.GET, "/api/secured")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(401, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Parse the response body as JSON
                    JsonObject json = new JsonObject(body);
                    
                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals("Unauthorized", json.getString("error"));
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testSecuredEndpointWithAuthentication(VertxTestContext testContext) {
        // Generate a token for a user with no roles
        String token = securityManager.generateToken("test-user", new String[]{});
        
        // Make a request to the secured endpoint with authentication
        client.request(HttpMethod.GET, "/api/secured")
                .compose(request -> request
                        .putHeader("Authorization", "Bearer " + token)
                        .send())
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
    void testAdminEndpointWithoutAdminRole(VertxTestContext testContext) {
        // Generate a token for a user with user role but not admin role
        String token = securityManager.generateToken("test-user", new String[]{"user"});
        
        // Make a request to the admin endpoint with user role
        client.request(HttpMethod.GET, "/api/admin")
                .compose(request -> request
                        .putHeader("Authorization", "Bearer " + token)
                        .send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(403, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Parse the response body as JSON
                    JsonObject json = new JsonObject(body);
                    
                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals("Forbidden", json.getString("error"));
                        assertEquals("Insufficient permissions", json.getString("message"));
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testAdminEndpointWithAdminRole(VertxTestContext testContext) {
        // Generate a token for a user with admin role
        String token = securityManager.generateToken("admin-user", new String[]{"admin"});
        
        // Make a request to the admin endpoint with admin role
        client.request(HttpMethod.GET, "/api/admin")
                .compose(request -> request
                        .putHeader("Authorization", "Bearer " + token)
                        .send())
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
                        assertEquals("admin", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("admin-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testUserEndpointWithUserRole(VertxTestContext testContext) {
        // Generate a token for a user with user role
        String token = securityManager.generateToken("regular-user", new String[]{"user"});
        
        // Make a request to the user endpoint with user role
        client.request(HttpMethod.GET, "/api/user")
                .compose(request -> request
                        .putHeader("Authorization", "Bearer " + token)
                        .send())
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
                        assertEquals("user", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("regular-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
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
    
    // Handler for admin endpoint
    private void handleAdminEndpoint(RoutingContext context) {
        String userId = context.user().principal().getString("sub");
        
        context.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("endpoint", "admin")
                        .put("status", "success")
                        .put("user", userId)
                        .encode());
    }
    
    // Handler for user endpoint
    private void handleUserEndpoint(RoutingContext context) {
        String userId = context.user().principal().getString("sub");
        
        context.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("endpoint", "user")
                        .put("status", "success")
                        .put("user", userId)
                        .encode());
    }
}