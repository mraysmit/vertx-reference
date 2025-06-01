package dev.mars.vertx.gateway.handler;

import dev.mars.vertx.gateway.service.MicroserviceClient;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
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

@ExtendWith(VertxExtension.class)
class ServiceHandlersTest {

    private Vertx vertx;
    private HttpServer server;
    private int port;
    private MockMicroserviceClient serviceOneClient;
    private MockMicroserviceClient serviceTwoClient;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        serviceOneClient = new MockMicroserviceClient(vertx, "service-one");
        serviceTwoClient = new MockMicroserviceClient(vertx, "service-two");

        // Create a router with the service handlers
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Add Service One handler
        ServiceOneHandler serviceOneHandler = new ServiceOneHandler(serviceOneClient);
        router.get("/api/service-one/:id").handler(serviceOneHandler::handle);
        router.post("/api/service-one").handler(serviceOneHandler::handle);

        // Add Service Two handler
        ServiceTwoHandler serviceTwoHandler = new ServiceTwoHandler(serviceTwoClient);
        router.get("/api/service-two/:id").handler(serviceTwoHandler::handle);
        router.post("/api/service-two").handler(serviceTwoHandler::handle);

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
     * Provides test data for the parameterized service handler test.
     * Each argument contains:
     * 1. Service name
     * 2. Mock client
     * 3. API endpoint path
     */
    static Stream<Arguments> serviceHandlerTestData() {
        return Stream.of(
                Arguments.of("service-one", "serviceOneClient", "/api/service-one/test-id"),
                Arguments.of("service-two", "serviceTwoClient", "/api/service-two/test-id")
        );
    }

    @ParameterizedTest
    @MethodSource("serviceHandlerTestData")
    void testServiceGetRequest(String serviceName, String clientFieldName, String endpoint, VertxTestContext testContext) throws Exception {
        // Get the appropriate mock client using reflection
        MockMicroserviceClient mockClient = (MockMicroserviceClient) this.getClass()
                .getDeclaredField(clientFieldName)
                .get(this);

        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("id", "test-id")
                .put("name", "Test Item")
                .put("description", "Test Description");
        mockClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the service endpoint
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
                    assertEquals("test-id", json.getString("id"));
                    assertEquals("Test Item", json.getString("name"));
                    assertEquals("Test Description", json.getString("description"));

                    // Verify that the client was called with the correct request
                    JsonObject clientRequest = mockClient.getLastRequest();
                    assertNotNull(clientRequest);
                    assertEquals("test-id", clientRequest.getString("id"));

                    // Verify that the service name is correct
                    assertEquals(serviceName, mockClient.getServiceName());

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
