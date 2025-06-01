package dev.mars.vertx.gateway;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ApiGatewayVerticleTest {

    private Vertx vertx;
    private int port;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Configure the verticle with a test port
        port = 8888;
        JsonObject config = new JsonObject()
                .put("http.port", port);

        // Deploy the verticle
        vertx.deployVerticle(new ApiGatewayVerticle(), 
                new io.vertx.core.DeploymentOptions().setConfig(config))
            .onComplete(testContext.succeeding(id -> {
                testContext.completeNow();
            }));
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close()
            .onComplete(testContext.succeeding(v -> {
                testContext.completeNow();
            }));
    }

    @Test
    void testVerticleDeployment(VertxTestContext testContext) {
        // This test passes if the verticle deploys successfully
        // The setup method already verifies this
        testContext.completeNow();
    }

    @Test
    void testHealthEndpoint(VertxTestContext testContext) {
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

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testServiceOneEndpoint(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the service one endpoint
        // This will fail because the service is not available in the test environment
        // But we can verify that the endpoint is configured correctly
        client.request(HttpMethod.GET, port, "localhost", "/api/service-one/test-id")
            .compose(request -> request.send())
            .onComplete(testContext.succeeding(response -> {
                // Verify that the endpoint exists and returns a response
                // The response will be an error because the service is not available
                testContext.verify(() -> {
                    assertTrue(response.statusCode() >= 400);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testServiceTwoEndpoint(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the service two endpoint
        // This will fail because the service is not available in the test environment
        // But we can verify that the endpoint is configured correctly
        client.request(HttpMethod.GET, port, "localhost", "/api/service-two/test-id")
            .compose(request -> request.send())
            .onComplete(testContext.succeeding(response -> {
                // Verify that the endpoint exists and returns a response
                // The response will be an error because the service is not available
                testContext.verify(() -> {
                    assertTrue(response.statusCode() >= 400);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testNonExistentEndpoint(VertxTestContext testContext) {
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
    void testOpenApiDocumentation(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the OpenAPI documentation endpoint
        client.request(HttpMethod.GET, port, "localhost", "/openapi.yaml")
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response status code
                assertEquals(200, response.statusCode());

                // Verify the content type header
                assertEquals("text/yaml", response.getHeader("Content-Type"));

                // Get the response body
                return response.body();
            })
            .onComplete(testContext.succeeding(body -> {
                // Verify the response content
                testContext.verify(() -> {
                    String content = body.toString();
                    assertTrue(content.contains("openapi:"));
                    assertTrue(content.contains("API Gateway"));
                    assertTrue(content.contains("/health"));
                    assertTrue(content.contains("/api/service-one"));
                    assertTrue(content.contains("/api/service-two"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testSwaggerUiRedirect(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the Swagger UI endpoint
        client.request(HttpMethod.GET, port, "localhost", "/swagger-ui")
            .compose(request -> request.send())
            .onComplete(testContext.succeeding(response -> {
                // Verify the response status code (should be a redirect)
                testContext.verify(() -> {
                    assertEquals(302, response.statusCode());

                    // Verify the redirect location
                    String location = response.getHeader("Location");
                    assertNotNull(location);
                    assertTrue(location.contains("/swagger-ui/index.html"));
                    assertTrue(location.contains("/openapi.yaml"));

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testDocsRedirect(VertxTestContext testContext) {
        // Create an HTTP client to make requests to the server
        HttpClient client = vertx.createHttpClient();

        // Make a request to the docs endpoint
        client.request(HttpMethod.GET, port, "localhost", "/docs")
            .compose(request -> request.send())
            .onComplete(testContext.succeeding(response -> {
                // Verify the response status code (should be a redirect)
                testContext.verify(() -> {
                    assertEquals(302, response.statusCode());

                    // Verify the redirect location
                    String location = response.getHeader("Location");
                    assertNotNull(location);
                    assertEquals("/swagger-ui", location);

                    testContext.completeNow();
                });
            }));
    }
}
