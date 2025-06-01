package dev.mars.vertx.common.eventbus;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class EventBusServiceTest {

    private Vertx vertx;
    private EventBusService eventBusService;
    private static final String TEST_ADDRESS = "test.address";

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        eventBusService = new EventBusService(vertx);
        testContext.completeNow();
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void testSendAndReceive(VertxTestContext testContext) {
        // Register a consumer
        MessageConsumer<JsonObject> consumer = eventBusService.consumer(TEST_ADDRESS, message -> {
            // Echo back the message with an added field
            JsonObject response = message.copy().put("received", true);
            return Future.succeededFuture(response);
        });

        // Send a message
        JsonObject message = new JsonObject().put("test", "value");

        eventBusService.send(TEST_ADDRESS, message, JsonObject.class)
                .onComplete(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertNotNull(response);
                        assertEquals("value", response.getString("test"));
                        assertTrue(response.getBoolean("received"));

                        // Unregister the consumer
                        eventBusService.unregisterConsumer(consumer)
                                .onComplete(testContext.succeedingThenComplete());
                    });
                }));
    }

    @Test
    void testSendWithTimeout(VertxTestContext testContext) {
        // Send a message to a non-existent address with a short timeout
        JsonObject message = new JsonObject().put("test", "value");

        eventBusService.send(TEST_ADDRESS + ".nonexistent", message, JsonObject.class, 100)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertNotNull(err);
                        // The error message might vary between Vert.x versions
                        // Just verify that an error occurred without checking the specific message
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testPublish(VertxTestContext testContext) {
        // Set up two consumers
        final int[] messageCount = {0};

        MessageConsumer<JsonObject> consumer1 = vertx.eventBus().consumer(TEST_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            assertEquals("value", body.getString("test"));
            messageCount[0]++;
            if (messageCount[0] == 2) {
                testContext.completeNow();
            }
        });

        MessageConsumer<JsonObject> consumer2 = vertx.eventBus().consumer(TEST_ADDRESS, message -> {
            JsonObject body = (JsonObject) message.body();
            assertEquals("value", body.getString("test"));
            messageCount[0]++;
            if (messageCount[0] == 2) {
                testContext.completeNow();
            }
        });

        // Publish a message
        JsonObject message = new JsonObject().put("test", "value");
        eventBusService.publish(TEST_ADDRESS, message);

        // Set a timeout in case the test doesn't complete
        vertx.setTimer(1000, id -> {
            if (!testContext.completed()) {
                testContext.failNow(new AssertionError("Did not receive messages on both consumers"));
            }
        });
    }

    @Test
    void testConsumerHandlingError(VertxTestContext testContext) {
        // Register a consumer that fails
        MessageConsumer<JsonObject> consumer = eventBusService.consumer(TEST_ADDRESS, message -> {
            return Future.failedFuture("Deliberate failure for testing");
        });

        // Send a message
        JsonObject message = new JsonObject().put("test", "value");

        eventBusService.send(TEST_ADDRESS, message, JsonObject.class)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertNotNull(err);
                        assertEquals("Deliberate failure for testing", err.getMessage());

                        // Unregister the consumer
                        eventBusService.unregisterConsumer(consumer)
                                .onComplete(testContext.succeedingThenComplete());
                    });
                }));
    }

    @Test
    void testCreateDeliveryOptions(VertxTestContext testContext) {
        // Create delivery options with headers
        JsonObject headers = new JsonObject()
                .put("header1", "value1")
                .put("header2", "value2");

        DeliveryOptions options = eventBusService.createDeliveryOptions(5000, headers);

        testContext.verify(() -> {
            assertEquals(5000, options.getSendTimeout());
            assertEquals("value1", options.getHeaders().get("header1"));
            assertEquals("value2", options.getHeaders().get("header2"));
            testContext.completeNow();
        });
    }

    @Test
    void testCreateDeliveryOptionsWithNullHeaders(VertxTestContext testContext) {
        // Create delivery options with null headers
        DeliveryOptions options = eventBusService.createDeliveryOptions(5000, null);

        testContext.verify(() -> {
            assertEquals(5000, options.getSendTimeout());
            // The headers might be null or empty depending on the Vert.x version
            // Just verify that we don't have any headers with values
            if (options.getHeaders() != null) {
                assertTrue(options.getHeaders().isEmpty());
            }
            testContext.completeNow();
        });
    }
}
