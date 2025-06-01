package dev.mars.vertx.gateway.router;

import dev.mars.vertx.gateway.handler.RequestHandler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class RouterFactoryTest {

    private Vertx vertx;
    private HttpServer server;
    private int port;
    private RouterFactory routerFactory;

    private TestHealthCheckHandler healthCheckHandler;
    private TestServiceHandler serviceOneHandler;
    private TestServiceHandler serviceTwoHandler;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Create test handlers
        healthCheckHandler = new TestHealthCheckHandler();
        serviceOneHandler = new TestServiceHandler("service-one");
        serviceTwoHandler = new TestServiceHandler("service-two");

        // Create a configuration with CORS enabled
        JsonObject config = new JsonObject()
                .put("cors", new JsonObject()
                        .put("enabled", true)
                        .put("allowed-origin", "*")
                        .put("allowed-headers", new JsonArray().add("Custom-Header"))
                        .put("allowed-methods", new JsonArray().add("PATCH")));

        routerFactory = new RouterFactory(vertx, config);

        // Create a router with the test handlers
        routerFactory.createRouter(
                healthCheckHandler,
                serviceOneHandler,
                serviceTwoHandler)
            .onSuccess(router -> {
                // Start a test HTTP server
                server = vertx.createHttpServer();
                server.requestHandler(router)
                    .listen(0) // Use port 0 to get a random available port
                    .onComplete(testContext.succeeding(httpServer -> {
                        port = httpServer.actualPort();
                        testContext.completeNow();
                    }));
            })
            .onFailure(err -> {
                testContext.failNow(err);
            });
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
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
    void testHealthCheckEndpoint(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the health check endpoint
        client.request(HttpMethod.GET, port, "localhost", "/health")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(200, response.statusCode());

                // Verify the content type header
                assertEquals("application/json", response.getHeader("Content-Type"));

                // Get the response body
                return response.body();
            })
            .onComplete(testContext.succeeding(body -> {
                // Parse the response body as JSON
                JsonObject json = new JsonObject(body);

                // Verify the response content
                testContext.verify(() -> {
                    assertEquals("UP", json.getString("status"));

                    // Verify that the handler was called
                    assertTrue(healthCheckHandler.wasCalled());

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testServiceOneEndpoint(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the service one endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/service-one/test-id")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(200, response.statusCode());

                // Verify the content type header
                assertEquals("application/json", response.getHeader("Content-Type"));

                // Get the response body
                return response.body();
            })
            .onComplete(testContext.succeeding(body -> {
                // Parse the response body as JSON
                JsonObject json = new JsonObject(body);

                // Verify the response content
                testContext.verify(() -> {
                    assertEquals("test-id", json.getString("id"));
                    assertEquals("service-one", json.getString("service"));

                    // Verify that the handler was called
                    assertTrue(serviceOneHandler.wasCalled());
                    assertEquals("test-id", serviceOneHandler.getLastId());

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testServiceTwoEndpoint(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the service two endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/service-two/test-id")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(200, response.statusCode());

                // Verify the content type header
                assertEquals("application/json", response.getHeader("Content-Type"));

                // Get the response body
                return response.body();
            })
            .onComplete(testContext.succeeding(body -> {
                // Parse the response body as JSON
                JsonObject json = new JsonObject(body);

                // Verify the response content
                testContext.verify(() -> {
                    assertEquals("test-id", json.getString("id"));
                    assertEquals("service-two", json.getString("service"));

                    // Verify that the handler was called
                    assertTrue(serviceTwoHandler.wasCalled());
                    assertEquals("test-id", serviceTwoHandler.getLastId());

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testCorsSupport(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make an OPTIONS request to test CORS
        client.request(HttpMethod.OPTIONS, port, "localhost", "/api/service-one/test-id")
            .compose(request -> request
                .putHeader("Origin", "http://example.com")
                .putHeader("Access-Control-Request-Method", "GET")
                .send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(204, response.statusCode());

                // Verify the CORS headers
                assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
                assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("GET"));
                assertTrue(response.getHeader("Access-Control-Allow-Methods").contains("PATCH"));
                assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("Custom-Header"));

                testContext.completeNow();
                return null;
            })
            .onFailure(testContext::failNow);
    }

    @Test
    void testFallbackHandler(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to a non-existent endpoint
        client.request(HttpMethod.GET, port, "localhost", "/non-existent")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(404, response.statusCode());

                // Verify the content type header
                assertEquals("application/json", response.getHeader("Content-Type"));

                // Get the response body
                return response.body();
            })
            .onComplete(testContext.succeeding(body -> {
                // Parse the response body as JSON
                JsonObject json = new JsonObject(body);

                // Verify the response content
                testContext.verify(() -> {
                    assertEquals("Not Found", json.getString("error"));
                    assertTrue(json.getString("message").contains("/non-existent"));
                    assertEquals("/non-existent", json.getString("path"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testErrorHandler(VertxTestContext testContext) {
        // Set the service one handler to fail
        serviceOneHandler.setShouldFail(true);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the service one endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/service-one/test-id")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(500, response.statusCode());

                // Verify the content type header
                assertEquals("application/json", response.getHeader("Content-Type"));

                // Get the response body
                return response.body();
            })
            .onComplete(testContext.succeeding(body -> {
                // Parse the response body as JSON
                JsonObject json = new JsonObject(body);

                // Verify the response content
                testContext.verify(() -> {
                    assertEquals("Internal Server Error", json.getString("error"));
                    assertTrue(json.getString("message").contains("Test error"));
                    assertEquals("/api/service-one/test-id", json.getString("path"));

                    // Verify that the handler was called
                    assertTrue(serviceOneHandler.wasCalled());

                    testContext.completeNow();
                });
            }));
    }

    /**
     * Test implementation of RequestHandler for health check.
     */
    private static class TestHealthCheckHandler implements RequestHandler {
        private boolean called = false;

        @Override
        public void handle(RoutingContext context) {
            called = true;
            context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("status", "UP")
                    .put("timestamp", System.currentTimeMillis())
                    .encode());
        }

        public boolean wasCalled() {
            return called;
        }
    }

    /**
     * Test implementation of RequestHandler for services.
     */
    private static class TestServiceHandler implements RequestHandler {
        private boolean called = false;
        private String lastId = null;
        private final String serviceName;
        private boolean shouldFail = false;

        public TestServiceHandler(String serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void handle(RoutingContext context) {
            called = true;

            if (shouldFail) {
                context.fail(500, new RuntimeException("Test error"));
                return;
            }

            String id = context.pathParam("id");
            if (id != null) {
                lastId = id;
            }

            context.response()
                .setStatusCode(200)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                    .put("id", id)
                    .put("service", serviceName)
                    .encode());
        }

        public boolean wasCalled() {
            return called;
        }

        public String getLastId() {
            return lastId;
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
    }
}
