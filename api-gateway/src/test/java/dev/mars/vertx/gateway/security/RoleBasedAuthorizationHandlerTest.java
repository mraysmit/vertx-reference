package dev.mars.vertx.gateway.security;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.PermissionBasedAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the RoleBasedAuthorizationHandler class.
 * Tests role-based authorization with different role configurations.
 */
@ExtendWith(VertxExtension.class)
class RoleBasedAuthorizationHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(RoleBasedAuthorizationHandlerTest.class);
    private static final int TEST_PORT = 9997;
    private static final String TEST_HOST = "localhost";

    private Vertx vertx;
    private HttpClient client;
    private HttpServer server;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Create HTTP client
        client = vertx.createHttpClient();

        // Create a router with secured endpoints
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Public endpoint - no authentication required
        router.get("/api/public").handler(this::handlePublicEndpoint);

        // Admin endpoint - admin role required
        router.get("/api/admin")
                .handler(this::setMockUser)
                .handler(new RoleBasedAuthorizationHandler("admin"))
                .handler(this::handleAdminEndpoint);

        // User endpoint - user role required
        router.get("/api/user")
                .handler(this::setMockUser)
                .handler(new RoleBasedAuthorizationHandler("user"))
                .handler(this::handleUserEndpoint);

        // Multi-role endpoint - requires both admin and user roles
        router.get("/api/multi-role")
                .handler(this::setMockUser)
                .handler(new RoleBasedAuthorizationHandler(true, "admin", "user"))
                .handler(this::handleMultiRoleEndpoint);

        // Any-role endpoint - requires either admin or user role
        router.get("/api/any-role")
                .handler(this::setMockUser)
                .handler(new RoleBasedAuthorizationHandler(false, "admin", "user"))
                .handler(this::handleAnyRoleEndpoint);

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
        // Make a request to the public endpoint
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
    void testAdminEndpointWithAdminRole(VertxTestContext testContext) {
        // Make a request to the admin endpoint with admin role
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/admin")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "admin");
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
                        assertEquals("admin", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("test-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testAdminEndpointWithoutAdminRole(VertxTestContext testContext) {
        // Make a request to the admin endpoint without admin role
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/admin")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "user");
                    return request.send();
                })
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
    void testUserEndpointWithUserRole(VertxTestContext testContext) {
        // Make a request to the user endpoint with user role
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/user")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "user");
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
                        assertEquals("user", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("test-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testMultiRoleEndpointWithBothRoles(VertxTestContext testContext) {
        // Make a request to the multi-role endpoint with both roles
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/multi-role")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "admin,user");
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
                        assertEquals("multi-role", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("test-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testMultiRoleEndpointWithOnlyOneRole(VertxTestContext testContext) {
        // Make a request to the multi-role endpoint with only one role
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/multi-role")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "admin");
                    return request.send();
                })
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
    void testAnyRoleEndpointWithAdminRole(VertxTestContext testContext) {
        // Make a request to the any-role endpoint with admin role
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/any-role")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "admin");
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
                        assertEquals("any-role", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("test-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testAnyRoleEndpointWithUserRole(VertxTestContext testContext) {
        // Make a request to the any-role endpoint with user role
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/any-role")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "user");
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
                        assertEquals("any-role", json.getString("endpoint"));
                        assertEquals("success", json.getString("status"));
                        assertEquals("test-user", json.getString("user"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testAnyRoleEndpointWithNoRoles(VertxTestContext testContext) {
        // Make a request to the any-role endpoint with no roles
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/any-role")
                .compose(request -> {
                    request.putHeader("X-User-Roles", "");
                    return request.send();
                })
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
    void testNoAuthenticatedUser(VertxTestContext testContext) {
        // Make a request to the admin endpoint without setting a user
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/admin")
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
                        assertEquals("Authentication required", json.getString("message"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testGetters() {
        // Create a handler with multiple roles and requireAllRoles=true
        RoleBasedAuthorizationHandler handler = new RoleBasedAuthorizationHandler(true, "admin", "user", "manager");
        
        // Test the getters
        Set<String> requiredRoles = handler.getRequiredRoles();
        assertTrue(requiredRoles.contains("admin"));
        assertTrue(requiredRoles.contains("user"));
        assertTrue(requiredRoles.contains("manager"));
        assertEquals(3, requiredRoles.size());
        assertTrue(handler.isRequireAllRoles());
        
        // Create a handler with a single role and default requireAllRoles (false)
        RoleBasedAuthorizationHandler singleRoleHandler = new RoleBasedAuthorizationHandler("admin");
        
        // Test the getters
        requiredRoles = singleRoleHandler.getRequiredRoles();
        assertTrue(requiredRoles.contains("admin"));
        assertEquals(1, requiredRoles.size());
        assertFalse(singleRoleHandler.isRequireAllRoles());
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

    // Handler for multi-role endpoint
    private void handleMultiRoleEndpoint(RoutingContext context) {
        String userId = context.user().principal().getString("sub");

        context.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("endpoint", "multi-role")
                        .put("status", "success")
                        .put("user", userId)
                        .encode());
    }

    // Handler for any-role endpoint
    private void handleAnyRoleEndpoint(RoutingContext context) {
        String userId = context.user().principal().getString("sub");

        context.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("endpoint", "any-role")
                        .put("status", "success")
                        .put("user", userId)
                        .encode());
    }

    // Helper method to set a mock user in the context
    private void setMockUser(RoutingContext context) {
        // Get roles from header
        String rolesHeader = context.request().getHeader("X-User-Roles");
        
        // If no roles header, don't set a user (to test the no user case)
        if (rolesHeader == null) {
            context.next();
            return;
        }
        
        // Create a mock user with the specified roles
        JsonObject principal = new JsonObject()
                .put("sub", "test-user")
                .put("roles", rolesHeader);
        
        // Set the user in the context
        context.setUser(User.create(principal));
        
        context.next();
    }
}