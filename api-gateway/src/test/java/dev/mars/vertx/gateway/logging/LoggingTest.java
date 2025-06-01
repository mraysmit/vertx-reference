package dev.mars.vertx.gateway.logging;

import ch.qos.logback.classic.Level;
import dev.mars.vertx.gateway.handler.AbstractRequestHandler;
import dev.mars.vertx.gateway.handler.HealthCheckHandler;
import dev.mars.vertx.gateway.security.JwtAuthHandler;
import dev.mars.vertx.gateway.security.RoleBasedAuthorizationHandler;
import dev.mars.vertx.gateway.security.SecurityManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
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
 * Tests for log verification.
 * Verifies that appropriate log messages are generated during API Gateway operations.
 */
@ExtendWith(VertxExtension.class)
class LoggingTest {

    private static final Logger logger = LoggerFactory.getLogger(LoggingTest.class);
    private static final int TEST_PORT = 9998;
    private static final String TEST_HOST = "localhost";
    
    private Vertx vertx;
    private HttpClient client;
    private LogCaptureAppender logCaptureAppender;
    
    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        
        // Configure HTTP client
        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(TEST_HOST)
                .setDefaultPort(TEST_PORT);
        
        client = vertx.createHttpClient(options);
        
        // Set up log capture
        logCaptureAppender = LogCaptureAppender.getInstance();
        logCaptureAppender.clearEvents();
        
        // Attach to loggers we want to monitor
        logCaptureAppender.attachToLogger(AbstractRequestHandler.class.getName());
        logCaptureAppender.attachToLogger(HealthCheckHandler.class.getName());
        logCaptureAppender.attachToLogger(SecurityManager.class.getName());
        logCaptureAppender.attachToLogger(JwtAuthHandler.class.getName());
        logCaptureAppender.attachToLogger(RoleBasedAuthorizationHandler.class.getName());
        
        // Create a router with endpoints that generate logs
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        
        // Health check endpoint
        HealthCheckHandler healthCheckHandler = new HealthCheckHandler();
        router.get("/health").handler(healthCheckHandler::handle);
        
        // Error endpoint
        router.get("/error").handler(ctx -> {
            throw new RuntimeException("Test error message");
        });
        
        // Security endpoints
        JsonObject config = new JsonObject()
                .put("security", new JsonObject()
                        .put("enabled", true)
                        .put("jwt", new JsonObject()
                                .put("symmetric-key", "test-symmetric-key-for-jwt-authentication")));
        
        SecurityManager securityManager = new SecurityManager(vertx, config);
        
        // Secured endpoint
        router.get("/secured")
                .handler(securityManager.getJwtAuthHandler())
                .handler(ctx -> {
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .end(new JsonObject()
                                    .put("status", "success")
                                    .encode());
                });
        
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
        // Detach from loggers
        logCaptureAppender.detachFromLogger(AbstractRequestHandler.class.getName());
        logCaptureAppender.detachFromLogger(HealthCheckHandler.class.getName());
        logCaptureAppender.detachFromLogger(SecurityManager.class.getName());
        logCaptureAppender.detachFromLogger(JwtAuthHandler.class.getName());
        logCaptureAppender.detachFromLogger(RoleBasedAuthorizationHandler.class.getName());
        
        client.close();
        vertx.close()
                .onComplete(testContext.succeeding(v -> {
                    testContext.completeNow();
                }));
    }
    
    @Test
    void testHealthCheckLogging(VertxTestContext testContext) {
        // Make a request to the health check endpoint
        client.request(HttpMethod.GET, "/health")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Verify that appropriate log messages were generated
                    testContext.verify(() -> {
                        // Check for debug log message from HealthCheckHandler
                        assertTrue(logCaptureAppender.hasEventWithLevelContainingMessage(
                                Level.DEBUG, "Handling health check request"));
                        
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testErrorLogging(VertxTestContext testContext) {
        // Make a request to the error endpoint
        client.request(HttpMethod.GET, "/error")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(500, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Verify that appropriate log messages were generated
                    testContext.verify(() -> {
                        // Check for error log message from AbstractRequestHandler
                        assertTrue(logCaptureAppender.hasEventWithLevelContainingMessage(
                                Level.ERROR, "Error handling request"));
                        
                        // Check that the error message contains the exception message
                        assertTrue(logCaptureAppender.hasEventContainingMessage("Test error message"));
                        
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testSecurityLogging(VertxTestContext testContext) {
        // Make a request to the secured endpoint without authentication
        client.request(HttpMethod.GET, "/secured")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(401, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Verify that appropriate log messages were generated
                    testContext.verify(() -> {
                        // Check for debug log message from JwtAuthHandler
                        assertTrue(logCaptureAppender.hasEventWithLevelContainingMessage(
                                Level.DEBUG, "Handling JWT authentication for request"));
                        
                        // Check for warning log message about missing authentication
                        assertTrue(logCaptureAppender.hasEventWithLevelContainingMessage(
                                Level.WARN, "No authenticated user found"));
                        
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testLogLevels(VertxTestContext testContext) {
        // Verify that we have captured logs at different levels
        client.request(HttpMethod.GET, "/error")
                .compose(request -> request.send())
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        // Check that we have logs at different levels
                        assertFalse(logCaptureAppender.getEventsForLevel(Level.DEBUG).isEmpty(), 
                                "Should have DEBUG level logs");
                        
                        assertFalse(logCaptureAppender.getEventsForLevel(Level.INFO).isEmpty(), 
                                "Should have INFO level logs");
                        
                        assertFalse(logCaptureAppender.getEventsForLevel(Level.ERROR).isEmpty(), 
                                "Should have ERROR level logs");
                        
                        testContext.completeNow();
                    });
                }));
    }
    
    @Test
    void testLogMessageContent(VertxTestContext testContext) {
        // Make a request to the health check endpoint
        client.request(HttpMethod.GET, "/health")
                .compose(request -> request.send())
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        // Get all log events containing "health"
                        var healthLogs = logCaptureAppender.getEventsContainingMessage("health");
                        
                        // Verify that we have at least one log message about health
                        assertFalse(healthLogs.isEmpty(), "Should have logs about health check");
                        
                        // Verify the content of the first health log
                        var firstHealthLog = healthLogs.get(0);
                        assertTrue(firstHealthLog.getFormattedMessage().contains("health"), 
                                "Log message should contain 'health'");
                        
                        testContext.completeNow();
                    });
                }));
    }
}