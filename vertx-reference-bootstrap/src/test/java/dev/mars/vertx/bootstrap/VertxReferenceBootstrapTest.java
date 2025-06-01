package dev.mars.vertx.bootstrap;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the VertxReferenceBootstrap class.
 * Verifies that the bootstrap process correctly deploys all services
 * and that the services can communicate with each other.
 */
@ExtendWith(VertxExtension.class)
class VertxReferenceBootstrapTest {

    private static final Logger logger = LoggerFactory.getLogger(VertxReferenceBootstrapTest.class);
    private static final int API_GATEWAY_PORT = 8080;
    private static final String API_GATEWAY_HOST = "localhost";

    private VertxReferenceBootstrap bootstrap;
    private WebClient webClient;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        // Create a new bootstrap instance
        bootstrap = new VertxReferenceBootstrap();

        // Start the bootstrap process
        try {
            bootstrap.start();

            // Create a WebClient for making API calls
            WebClientOptions clientOptions = new WebClientOptions()
                    .setDefaultHost(API_GATEWAY_HOST)
                    .setDefaultPort(API_GATEWAY_PORT);
            webClient = WebClient.create(bootstrap.getVertx(), clientOptions);

            testContext.completeNow();
        } catch (Exception e) {
            testContext.failNow(e);
        }
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        // Shutdown the bootstrap
        bootstrap.shutdown();
        testContext.completeNow();
    }

    @Test
    void testHealthEndpoint(VertxTestContext testContext) {
        logger.info("Testing health endpoint");

        // Make a request to the health endpoint
        webClient.get("/health")
                .send()
                .onSuccess(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());

                    // Parse the response body as JSON
                    JsonObject json = response.bodyAsJsonObject();

                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals("UP", json.getString("status"));
                        assertTrue(json.containsKey("timestamp"));
                        testContext.completeNow();
                    });
                })
                .onFailure(err -> {
                    logger.error("Health check failed", err);
                    testContext.failNow(err);
                });
    }

    @Test
    void testServiceOneEndpoint(VertxTestContext testContext) {
        logger.info("Testing Service One endpoint");

        // Create a test item
        JsonObject item = new JsonObject()
                .put("action", "create")
                .put("name", "Test Item")
                .put("description", "This is a test item created by the test");

        // Make a request to create an item
        webClient.post("/api/service-one")
                .sendJsonObject(item)
                .onSuccess(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());

                    // Parse the response body as JSON
                    JsonObject json = response.bodyAsJsonObject();

                    // Verify the response content
                    testContext.verify(() -> {
                        assertNotNull(json.getString("id"));
                        assertEquals("Test Item", json.getString("name"));
                        assertEquals("This is a test item created by the test", json.getString("description"));

                        // Now get the item
                        String itemId = json.getString("id");
                        getAndVerifyItem(itemId, testContext);
                    });
                })
                .onFailure(err -> {
                    logger.error("Create item failed", err);
                    testContext.failNow(err);
                });
    }

    @Test
    void testServiceTwoEndpoint(VertxTestContext testContext) {
        logger.info("Testing Service Two endpoint");

        // Create a test request with an action
        JsonObject request = new JsonObject()
                .put("action", "cities");

        // Make a request to get list of cities
        webClient.post("/api/service-two")
                .sendJsonObject(request)
                .onSuccess(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());

                    // Parse the response body as JSON
                    JsonObject json = response.bodyAsJsonObject();

                    // Verify the response content
                    testContext.verify(() -> {
                        assertTrue(json.containsKey("cities"));
                        assertTrue(json.getJsonArray("cities").size() > 0);
                        assertTrue(json.getJsonArray("cities").contains("London"));
                        testContext.completeNow();
                    });
                })
                .onFailure(err -> {
                    logger.error("Get cities failed", err);
                    testContext.failNow(err);
                });
    }

    private void getAndVerifyItem(String itemId, VertxTestContext testContext) {
        webClient.get("/api/service-one/" + itemId)
                .send()
                .onSuccess(response -> {
                    // Verify the response status code
                    assertEquals(200, response.statusCode());

                    // Parse the response body as JSON
                    JsonObject json = response.bodyAsJsonObject();

                    // Verify the response content
                    testContext.verify(() -> {
                        assertEquals(itemId, json.getString("id"));
                        assertEquals("Test Item", json.getString("name"));
                        assertEquals("This is a test item created by the test", json.getString("description"));
                        testContext.completeNow();
                    });
                })
                .onFailure(err -> {
                    logger.error("Get item failed", err);
                    testContext.failNow(err);
                });
    }

}
