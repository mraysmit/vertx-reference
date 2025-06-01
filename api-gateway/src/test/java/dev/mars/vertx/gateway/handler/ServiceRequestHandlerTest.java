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

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ServiceRequestHandlerTest {

    private Vertx vertx;
    private HttpServer server;
    private int port;
    private MockMicroserviceClient mockClient;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        mockClient = new MockMicroserviceClient(vertx);

        // Create a router with the service request handler
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        TestServiceRequestHandler serviceHandler = new TestServiceRequestHandler(mockClient);
        router.get("/api/test/:id").handler(serviceHandler::handle);
        router.post("/api/test").handler(serviceHandler::handle);

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

    @Test
    void testHandleGetRequest(VertxTestContext testContext) {
        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("id", "test-id")
                .put("name", "Test Item")
                .put("description", "Test Description");
        mockClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the service endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/test/test-id")
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

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testHandlePostRequest(VertxTestContext testContext) {
        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("id", "new-id")
                .put("name", "New Item")
                .put("description", "New Description");
        mockClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Create a request body
        JsonObject requestBody = new JsonObject()
                .put("name", "New Item")
                .put("description", "New Description");

        // Make a POST request to the service endpoint
        client.request(HttpMethod.POST, port, "localhost", "/api/test")
            .compose(request -> request
                .putHeader("Content-Type", "application/json")
                .send(requestBody.toBuffer()))
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
                    assertEquals("new-id", json.getString("id"));
                    assertEquals("New Item", json.getString("name"));
                    assertEquals("New Description", json.getString("description"));

                    // Verify that the client was called with the correct request
                    JsonObject clientRequest = mockClient.getLastRequest();
                    assertNotNull(clientRequest);
                    assertEquals("New Item", clientRequest.getString("name"));
                    assertEquals("New Description", clientRequest.getString("description"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testHandleServiceError(VertxTestContext testContext) {
        // Set up the mock client to return an error
        mockClient.setNextError("Service unavailable");

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the service endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/test/test-id")
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
                    assertTrue(json.getString("message").contains("Service unavailable"));

                    testContext.completeNow();
                });
            }));
    }

    /**
     * Test implementation of ServiceRequestHandler for testing.
     */
    private static class TestServiceRequestHandler extends ServiceRequestHandler {
        public TestServiceRequestHandler(MicroserviceClient serviceClient) {
            super(serviceClient, "test-service");
        }
    }

    /**
     * Mock implementation of MicroserviceClient for testing.
     */
    private static class MockMicroserviceClient extends MicroserviceClient {
        private JsonObject nextResponse;
        private String nextError;
        private JsonObject lastRequest;

        public MockMicroserviceClient(Vertx vertx) {
            // Pass vertx but null for circuit breaker since we're overriding the sendRequest method
            super(vertx, null, "test-service");
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
