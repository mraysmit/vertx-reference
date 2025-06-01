package dev.mars.vertx.gateway.handler;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class AbstractRequestHandlerTest {

    private Vertx vertx;
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Create a router with the test handlers
        Router router = Router.router(vertx);

        // Add a test handler that returns a successful response
        TestSuccessHandler successHandler = new TestSuccessHandler();
        router.get("/test/success").handler(successHandler::handle);

        // Add a test handler that throws an exception
        TestErrorHandler errorHandler = new TestErrorHandler();
        router.get("/test/error").handler(errorHandler::handle);

        // Add a test handler that throws an IllegalArgumentException
        TestBadRequestHandler badRequestHandler = new TestBadRequestHandler();
        router.get("/test/badrequest").handler(badRequestHandler::handle);

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
    void testSuccessfulResponse(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the success endpoint
        client.request(HttpMethod.GET, port, "localhost", "/test/success")
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
                    assertEquals("success", json.getString("status"));
                    assertEquals("Test successful response", json.getString("message"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testInternalServerError(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the error endpoint
        client.request(HttpMethod.GET, port, "localhost", "/test/error")
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
                    assertEquals("Test error message", json.getString("message"));
                    assertTrue(json.getString("path").contains("/test/error"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testBadRequestError(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the bad request endpoint
        client.request(HttpMethod.GET, port, "localhost", "/test/badrequest")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(400, response.statusCode());

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
                    assertEquals("Bad request error", json.getString("message"));
                    assertTrue(json.getString("path").contains("/test/badrequest"));
                    testContext.completeNow();
                });
            }));
    }

    /**
     * Test implementation of AbstractRequestHandler that returns a successful response.
     */
    private static class TestSuccessHandler extends AbstractRequestHandler {
        @Override
        protected void handleRequest(RoutingContext context) {
            JsonObject response = new JsonObject()
                    .put("status", "success")
                    .put("message", "Test successful response");
            sendResponse(context, response);
        }
    }

    /**
     * Test implementation of AbstractRequestHandler that throws an exception.
     */
    private static class TestErrorHandler extends AbstractRequestHandler {
        @Override
        protected void handleRequest(RoutingContext context) {
            throw new RuntimeException("Test error message");
        }
    }

    /**
     * Test implementation of AbstractRequestHandler that throws an IllegalArgumentException.
     */
    private static class TestBadRequestHandler extends AbstractRequestHandler {
        @Override
        protected void handleRequest(RoutingContext context) {
            throw new IllegalArgumentException("Bad request error");
        }
    }
}
