package dev.mars.vertx.gateway.handler;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class HealthCheckHandlerTest {

    private Vertx vertx;
    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Create a router with the health check handler
        Router router = Router.router(vertx);
        HealthCheckHandler healthCheckHandler = new HealthCheckHandler();
        router.get("/health").handler(healthCheckHandler::handle);

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
                    assertTrue(json.containsKey("timestamp"));
                    assertTrue(json.getLong("timestamp") > 0);
                    testContext.completeNow();
                });
            }));
    }
}
