package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ServiceTwoHandler class.
 * Specifically tests the factory methods that create specialized handlers.
 */
@ExtendWith(VertxExtension.class)
class ServiceTwoHandlerTest {

    private Vertx vertx;
    private HttpServer server;
    private int port;
    private MockMicroserviceClient serviceTwoClient;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        serviceTwoClient = new MockMicroserviceClient(vertx, "service-two");

        // Create a router with the service handlers
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Add handlers created by factory methods
        // Note: Order matters! More specific routes must be defined before more general ones
        router.get("/api/service-two/cities").handler(
                ServiceTwoHandler.createCitiesHandler(serviceTwoClient));

        router.get("/api/service-two/forecast/:city").handler(
                ServiceTwoHandler.createForecastHandler(serviceTwoClient));

        router.get("/api/service-two/random").handler(
                ServiceTwoHandler.createRandomWeatherHandler(serviceTwoClient));

        router.get("/api/service-two/stats").handler(
                ServiceTwoHandler.createStatsHandler(serviceTwoClient));

        router.get("/api/service-two/custom").handler(
                ServiceTwoHandler.createActionHandler(serviceTwoClient, "custom"));

        // This must be last as it's a catch-all for IDs
        router.get("/api/service-two/:id").handler(
                ServiceTwoHandler.createWeatherItemHandler(serviceTwoClient));

        // Start a test HTTP server
        server = vertx.createHttpServer();
        server.requestHandler(router)
            .listen(0) // Use port 0 to get a random available port
            .onComplete(testContext.succeeding(httpServer -> {
                port = httpServer.actualPort();
                testContext.completeNow();
            }));
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

    /**
     * Provides test data for the parameterized factory method test.
     * Each argument contains:
     * 1. Endpoint path
     * 2. Expected action value
     */
    static Stream<Arguments> factoryMethodTestData() {
        return Stream.of(
                Arguments.of("/api/service-two/cities", "cities"),
                Arguments.of("/api/service-two/forecast/london", "forecast"),
                Arguments.of("/api/service-two/stats", "stats"),
                Arguments.of("/api/service-two/custom", "custom")
        );
    }

    @ParameterizedTest
    @MethodSource("factoryMethodTestData")
    void testFactoryMethods(String endpoint, String expectedAction, VertxTestContext testContext) {
        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("result", "success")
                .put("action", expectedAction);
        serviceTwoClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the endpoint
        client.request(HttpMethod.GET, port, "localhost", endpoint)
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
                    assertEquals("success", json.getString("result"));
                    assertEquals(expectedAction, json.getString("action"));

                    // Verify that the client was called with the correct request
                    JsonObject clientRequest = serviceTwoClient.getLastRequest();
                    assertNotNull(clientRequest);
                    assertEquals(expectedAction, clientRequest.getString("action"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testRandomWeatherHandler(VertxTestContext testContext) {
        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("result", "success")
                .put("type", "random");
        serviceTwoClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the random endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/service-two/random")
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
                    assertEquals("success", json.getString("result"));
                    assertEquals("random", json.getString("type"));

                    // Verify that the client was called
                    JsonObject clientRequest = serviceTwoClient.getLastRequest();
                    assertNotNull(clientRequest);
                    // Random weather handler doesn't set an action
                    assertFalse(clientRequest.containsKey("action"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testWeatherItemHandler(VertxTestContext testContext) {
        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("result", "success")
                .put("id", "test-id");
        serviceTwoClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the item endpoint
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
                    assertEquals("success", json.getString("result"));
                    assertEquals("test-id", json.getString("id"));

                    // Verify that the client was called with the correct request
                    JsonObject clientRequest = serviceTwoClient.getLastRequest();
                    assertNotNull(clientRequest);
                    assertEquals("test-id", clientRequest.getString("id"));
                    // Weather item handler doesn't set an action
                    assertFalse(clientRequest.containsKey("action"));

                    testContext.completeNow();
                });
            }));
    }

    /**
     * Mock implementation of MicroserviceClient for testing.
     */
    private static class MockMicroserviceClient extends MicroserviceClient {
        private JsonObject nextResponse;
        private String nextError;
        private JsonObject lastRequest;
        private final String serviceName;

        public MockMicroserviceClient(Vertx vertx, String serviceName) {
            // Pass vertx but null for circuit breaker since we're overriding the sendRequest method
            super(vertx, null, serviceName);
            this.serviceName = serviceName;
        }

        public void setNextResponse(JsonObject response) {
            this.nextResponse = response;
            this.nextError = null;
        }

        public void setNextError(String error) {
            this.nextError = error;
            this.nextResponse = null;
        }

        public JsonObject getLastRequest() {
            return lastRequest;
        }

        public String getServiceName() {
            return serviceName;
        }

        @Override
        public Future<JsonObject> sendRequest(JsonObject request) {
            this.lastRequest = request;

            if (nextError != null) {
                return Future.failedFuture(nextError);
            } else if (nextResponse != null) {
                return Future.succeededFuture(nextResponse);
            } else {
                return Future.failedFuture("No response or error configured");
            }
        }
    }
}
