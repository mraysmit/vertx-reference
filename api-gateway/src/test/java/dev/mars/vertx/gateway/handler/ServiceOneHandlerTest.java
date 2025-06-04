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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ServiceOneHandler class.
 * Tests the handling of requests to Service One.
 */
@ExtendWith(VertxExtension.class)
class ServiceOneHandlerTest {

    private static final Logger logger = LoggerFactory.getLogger(ServiceOneHandlerTest.class);
    private Vertx vertx;
    private HttpServer server;
    private int port;
    private MockMicroserviceClient mockClient;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        mockClient = new MockMicroserviceClient(vertx);

        // Create a router with the service one handler
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        ServiceOneHandler serviceOneHandler = new ServiceOneHandler(mockClient);
        router.get("/api/service-one/:id").handler(serviceOneHandler::handle);
        router.post("/api/service-one").handler(serviceOneHandler::handle);

        // Start a test HTTP server
        server = vertx.createHttpServer();
        server.requestHandler(router)
            .listen(0) // Use port 0 to get a random available port
            .onComplete(testContext.succeeding(httpServer -> {
                port = httpServer.actualPort();
                logger.info("Test server started on port {}", port);
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
                .put("id", "service-one-id")
                .put("name", "Service One Item")
                .put("description", "Service One Description");
        mockClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the service one endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/service-one/service-one-id")
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
                    assertEquals("service-one-id", json.getString("id"));
                    assertEquals("Service One Item", json.getString("name"));
                    assertEquals("Service One Description", json.getString("description"));

                    // Verify that the client was called with the correct request
                    JsonObject clientRequest = mockClient.getLastRequest();
                    assertNotNull(clientRequest);
                    assertEquals("service-one-id", clientRequest.getString("id"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testHandlePostRequest(VertxTestContext testContext) {
        // Set up the mock client to return a test response
        JsonObject testResponse = new JsonObject()
                .put("id", "new-service-one-id")
                .put("name", "New Service One Item")
                .put("description", "New Service One Description");
        mockClient.setNextResponse(testResponse);

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Create a request body
        JsonObject requestBody = new JsonObject()
                .put("name", "New Service One Item")
                .put("description", "New Service One Description");

        // Make a POST request to the service one endpoint
        client.request(HttpMethod.POST, port, "localhost", "/api/service-one")
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
                    assertEquals("new-service-one-id", json.getString("id"));
                    assertEquals("New Service One Item", json.getString("name"));
                    assertEquals("New Service One Description", json.getString("description"));

                    // Verify that the client was called with the correct request
                    JsonObject clientRequest = mockClient.getLastRequest();
                    assertNotNull(clientRequest);
                    assertEquals("New Service One Item", clientRequest.getString("name"));
                    assertEquals("New Service One Description", clientRequest.getString("description"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testHandleServiceError(VertxTestContext testContext) {
        // Set up the mock client to return an error
        mockClient.setNextError("Service One unavailable");

        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a GET request to the service one endpoint
        client.request(HttpMethod.GET, port, "localhost", "/api/service-one/service-one-id")
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
     * Mock implementation of MicroserviceClient for testing.
     */
    private static class MockMicroserviceClient extends MicroserviceClient {
        private JsonObject nextResponse;
        private String nextError;
        private JsonObject lastRequest;

        public MockMicroserviceClient(Vertx vertx) {
            // Pass vertx but null for circuit breaker since we're overriding the sendRequest method
            super(vertx, null, "service-one");
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