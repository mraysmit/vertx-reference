package dev.mars.vertx.gateway.integration;

import dev.mars.vertx.gateway.ApiGatewayVerticle;
import dev.mars.vertx.gateway.handler.HealthCheckHandler;
import dev.mars.vertx.gateway.handler.ServiceOneHandler;
import dev.mars.vertx.gateway.handler.ServiceTwoHandler;
import dev.mars.vertx.gateway.router.RouterFactory;
import dev.mars.vertx.gateway.service.MicroserviceClient;
import dev.mars.vertx.gateway.service.MicroserviceClientFactory;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the API Gateway.
 * Tests the interaction between multiple components and the end-to-end request flow.
 */
@ExtendWith(VertxExtension.class)
class IntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
    private static final int TEST_PORT = 8889;
    private static final String TEST_HOST = "localhost";

    private Vertx vertx;
    private HttpClient client;
    private MockServiceVerticle serviceOneVerticle;
    private MockServiceVerticle serviceTwoVerticle;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Create HTTP client
        client = vertx.createHttpClient();

        // Deploy mock service verticles
        serviceOneVerticle = new MockServiceVerticle("service.one");
        serviceTwoVerticle = new MockServiceVerticle("service.two");

        // Deploy the mock services first
        Future.all(
                vertx.deployVerticle(serviceOneVerticle),
                vertx.deployVerticle(serviceTwoVerticle)
        )
        .compose(v -> {
            // Create API Gateway configuration
            JsonObject config = new JsonObject()
                    .put("http.port", TEST_PORT)
                    .put("services", new JsonObject()
                            .put("service-one", new JsonObject()
                                    .put("address", "service.one"))
                            .put("service-two", new JsonObject()
                                    .put("address", "service.two")));

            // Deploy the API Gateway verticle
            return vertx.deployVerticle(
                    new ApiGatewayVerticle(),
                    new io.vertx.core.DeploymentOptions().setConfig(config)
            );
        })
        .onComplete(testContext.succeeding(id -> {
            logger.info("API Gateway and mock services deployed successfully");
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
    void testHealthEndpoint(VertxTestContext testContext) {
        // Make a request to the health endpoint
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/health")
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
                        assertEquals("UP", json.getString("status"));
                        assertTrue(json.containsKey("timestamp"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testServiceOneEndpoint(VertxTestContext testContext) {
        // Set up the mock service to return a specific response
        serviceOneVerticle.setNextResponse(new JsonObject()
                .put("id", "test-id")
                .put("name", "Test Item")
                .put("description", "Test Description"));

        // Make a request to the service one endpoint
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/service-one/test-id")
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
                        assertEquals("test-id", json.getString("id"));
                        assertEquals("Test Item", json.getString("name"));
                        assertEquals("Test Description", json.getString("description"));

                        // Verify that the mock service was called
                        assertEquals(1, serviceOneVerticle.getRequestCount());

                        // Verify that the request was correctly routed
                        JsonObject lastRequest = serviceOneVerticle.getLastRequest();
                        assertNotNull(lastRequest);
                        assertEquals("test-id", lastRequest.getString("id"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testServiceTwoEndpoint(VertxTestContext testContext) {
        // Set up the mock service to return a specific response
        serviceTwoVerticle.setNextResponse(new JsonObject()
                .put("id", "test-id-2")
                .put("name", "Test Item 2")
                .put("description", "Test Description 2"));

        // Make a request to the service two endpoint
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/service-two/test-id-2")
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
                        assertEquals("test-id-2", json.getString("id"));
                        assertEquals("Test Item 2", json.getString("name"));
                        assertEquals("Test Description 2", json.getString("description"));

                        // Verify that the mock service was called
                        assertEquals(1, serviceTwoVerticle.getRequestCount());

                        // Verify that the request was correctly routed
                        JsonObject lastRequest = serviceTwoVerticle.getLastRequest();
                        assertNotNull(lastRequest);
                        assertEquals("test-id-2", lastRequest.getString("id"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testServiceOnePostRequest(VertxTestContext testContext) {
        // Set up the mock service to return a specific response
        serviceOneVerticle.setNextResponse(new JsonObject()
                .put("id", "new-id")
                .put("name", "New Item")
                .put("description", "New Description"));

        // Create a request body
        JsonObject requestBody = new JsonObject()
                .put("name", "New Item")
                .put("description", "New Description");

        // Make a POST request to the service one endpoint
        client.request(HttpMethod.POST, TEST_PORT, TEST_HOST, "/api/service-one")
                .compose(request -> request
                        .putHeader("Content-Type", "application/json")
                        .send(requestBody.toBuffer()))
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
                        assertEquals("new-id", json.getString("id"));
                        assertEquals("New Item", json.getString("name"));
                        assertEquals("New Description", json.getString("description"));

                        // Verify that the mock service was called
                        assertEquals(1, serviceOneVerticle.getRequestCount());

                        // Verify that the request was correctly routed
                        JsonObject lastRequest = serviceOneVerticle.getLastRequest();
                        assertNotNull(lastRequest);
                        assertEquals("New Item", lastRequest.getString("name"));
                        assertEquals("New Description", lastRequest.getString("description"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testServiceUnavailable(VertxTestContext testContext) {
        // Set up the mock service to fail
        serviceOneVerticle.setFailureMode(true);

        // Make a request to the service one endpoint
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/api/service-one/test-id")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(500, response.statusCode());
                    return response.body();
                })
                .onComplete(testContext.succeeding(body -> {
                    // Parse the response body as JSON
                    JsonObject json = new JsonObject(body);

                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals("Internal Server Error", json.getString("error"));
                        assertTrue(json.getString("message").contains("Service unavailable"));

                        // Verify that the mock service was called
                        assertEquals(1, serviceOneVerticle.getRequestCount());

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testNonExistentEndpoint(VertxTestContext testContext) {
        // Make a request to a non-existent endpoint
        client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, "/non-existent")
                .compose(request -> request.send())
                .compose(response -> {
                    // Verify the response status code
                    assertEquals(404, response.statusCode());
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

    /**
     * A mock service verticle that simulates a microservice.
     */
    private static class MockServiceVerticle implements io.vertx.core.Verticle {
        private final String address;
        private Vertx vertx;
        private JsonObject nextResponse;
        private boolean failureMode = false;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private JsonObject lastRequest;

        public MockServiceVerticle(String address) {
            this.address = address;
        }

        @Override
        public Vertx getVertx() {
            return vertx;
        }

        @Override
        public void init(Vertx vertx, io.vertx.core.Context context) {
            this.vertx = vertx;
        }

        @Override
        public void start(io.vertx.core.Promise<Void> startPromise) {
            // Register a consumer for the service address
            vertx.eventBus().consumer(address, message -> {
                requestCount.incrementAndGet();
                lastRequest = (JsonObject) message.body();

                if (failureMode) {
                    message.fail(500, "Service unavailable");
                } else if (nextResponse != null) {
                    message.reply(nextResponse);
                } else {
                    // Default response
                    JsonObject response = new JsonObject()
                            .put("id", lastRequest.getString("id", "default-id"))
                            .put("name", "Default Item")
                            .put("description", "Default Description");
                    message.reply(response);
                }
            });

            startPromise.complete();
        }

        @Override
        public void stop(io.vertx.core.Promise<Void> stopPromise) {
            stopPromise.complete();
        }

        /**
         * Sets the next response to return.
         *
         * @param response the response to return
         */
        public void setNextResponse(JsonObject response) {
            this.nextResponse = response;
            this.failureMode = false;
        }

        /**
         * Sets the failure mode.
         *
         * @param failureMode true to make the service fail, false otherwise
         */
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }

        /**
         * Gets the number of requests received.
         *
         * @return the request count
         */
        public int getRequestCount() {
            return requestCount.get();
        }

        /**
         * Gets the last request received.
         *
         * @return the last request
         */
        public JsonObject getLastRequest() {
            return lastRequest;
        }
    }
}
