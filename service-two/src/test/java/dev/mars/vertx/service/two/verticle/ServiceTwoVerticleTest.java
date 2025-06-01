package dev.mars.vertx.service.two.verticle;

import dev.mars.vertx.service.two.ServiceTwoVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ServiceTwoVerticleTest {

    private static final Logger logger = LoggerFactory.getLogger(ServiceTwoVerticleTest.class);
    private static final String SERVICE_ADDRESS = "service.two.test";

    private Vertx vertx;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();

        // Deploy the verticle with a test configuration
        JsonObject config = new JsonObject()
                .put("service.address", SERVICE_ADDRESS);

        vertx.deployVerticle(new ServiceTwoVerticle(), 
                new io.vertx.core.DeploymentOptions().setConfig(config))
            .onComplete(testContext.succeeding(id -> {
                logger.info("ServiceTwoVerticle deployed with id: {}", id);
                testContext.completeNow();
            }));
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close(testContext.succeeding(v -> {
            logger.info("Vertx closed");
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
    void testGetWeatherForCity(VertxTestContext testContext) {
        // Create a request for weather in London
        JsonObject request = new JsonObject()
                .put("city", "London");

        // Send the request to the service
        vertx.eventBus().<JsonObject>request(SERVICE_ADDRESS, request)
            .onComplete(testContext.succeeding(response -> {
                testContext.verify(() -> {
                    JsonObject weather = response.body();
                    assertNotNull(weather);
                    assertEquals("London", weather.getString("city"));
                    assertTrue(weather.containsKey("temperature"));
                    assertTrue(weather.containsKey("humidity"));
                    assertTrue(weather.containsKey("windSpeed"));
                    assertTrue(weather.containsKey("condition"));
                    assertTrue(weather.containsKey("timestamp"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetRandomWeather(VertxTestContext testContext) {
        // Create an empty request (will default to random weather)
        JsonObject request = new JsonObject();

        // Send the request to the service
        vertx.eventBus().<JsonObject>request(SERVICE_ADDRESS, request)
            .onComplete(testContext.succeeding(response -> {
                testContext.verify(() -> {
                    JsonObject weather = response.body();
                    assertNotNull(weather);
                    assertTrue(weather.containsKey("city"));
                    assertTrue(weather.containsKey("temperature"));
                    assertTrue(weather.containsKey("humidity"));
                    assertTrue(weather.containsKey("windSpeed"));
                    assertTrue(weather.containsKey("condition"));
                    assertTrue(weather.containsKey("timestamp"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testListCities(VertxTestContext testContext) {
        // Create a request to list cities
        JsonObject request = new JsonObject()
                .put("action", "cities");

        // Send the request to the service
        vertx.eventBus().<JsonObject>request(SERVICE_ADDRESS, request)
            .onComplete(testContext.succeeding(response -> {
                testContext.verify(() -> {
                    JsonObject result = response.body();
                    assertNotNull(result);
                    assertTrue(result.containsKey("cities"));
                    assertTrue(result.containsKey("count"));
                    assertEquals(10, result.getInteger("count"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetForecast(VertxTestContext testContext) {
        // Create a request for a forecast
        JsonObject request = new JsonObject()
                .put("action", "forecast")
                .put("city", "London")
                .put("days", 5);

        // Send the request to the service
        vertx.eventBus().<JsonObject>request(SERVICE_ADDRESS, request)
            .onComplete(testContext.succeeding(response -> {
                testContext.verify(() -> {
                    JsonObject result = response.body();
                    assertNotNull(result);
                    assertEquals("London", result.getString("city"));
                    assertEquals(5, result.getInteger("days"));
                    assertTrue(result.containsKey("forecast"));
                    assertEquals(5, result.getJsonArray("forecast").size());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetStats(VertxTestContext testContext) {
        // Create a request to get stats
        JsonObject request = new JsonObject()
                .put("action", "stats");

        // Send the request to the service
        vertx.eventBus().<JsonObject>request(SERVICE_ADDRESS, request)
            .onComplete(testContext.succeeding(response -> {
                testContext.verify(() -> {
                    JsonObject result = response.body();
                    assertNotNull(result);
                    assertTrue(result.containsKey("requestsProcessed"));
                    assertTrue(result.containsKey("citiesAvailable"));
                    assertTrue(result.containsKey("uptime"));
                    assertEquals(10, result.getInteger("citiesAvailable"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUnknownAction(VertxTestContext testContext) {
        // Create a request with an unknown action
        JsonObject request = new JsonObject()
                .put("action", "unknown");

        // Send the request to the service
        vertx.eventBus().request(SERVICE_ADDRESS, request)
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("Unknown action"));
                    testContext.completeNow();
                });
            }));
    }
}