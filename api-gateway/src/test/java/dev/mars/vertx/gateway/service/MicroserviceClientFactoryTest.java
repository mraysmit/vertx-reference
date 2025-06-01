package dev.mars.vertx.gateway.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class MicroserviceClientFactoryTest {

    private Vertx vertx;
    private MicroserviceClientFactory factory;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();

        // Create a configuration with service settings
        JsonObject config = new JsonObject()
                .put("services", new JsonObject()
                        .put("service-one", new JsonObject()
                                .put("address", "service.one.address")
                                .put("circuit-breaker", new JsonObject()
                                        .put("max-failures", 3)
                                        .put("timeout", 5000L)
                                        .put("reset-timeout", 10000L)))
                        .put("service-two", new JsonObject()
                                .put("address", "service.two.address")
                                .put("circuit-breaker", new JsonObject()
                                        .put("max-failures", 5)
                                        .put("timeout", 7000L)
                                        .put("reset-timeout", 15000L))));

        factory = new MicroserviceClientFactory(vertx, config);
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close()
            .onComplete(testContext.succeeding(v -> {
                testContext.completeNow();
            }));
    }

    @Test
    void testGetClient() {
        // Get a client for service-one
        MicroserviceClient clientOne = factory.getClient("service-one");

        // Verify the client is not null
        assertNotNull(clientOne);

        // Verify the client is an instance of MicroserviceClient
        assertTrue(clientOne instanceof MicroserviceClient);
    }

    @Test
    void testClientReuse() {
        // Get a client for service-one
        MicroserviceClient clientOne1 = factory.getClient("service-one");

        // Get another client for service-one
        MicroserviceClient clientOne2 = factory.getClient("service-one");

        // Verify that the same client instance is returned
        assertSame(clientOne1, clientOne2);

        // Get a client for service-two
        MicroserviceClient clientTwo = factory.getClient("service-two");

        // Verify that a different client instance is returned for a different service
        assertNotSame(clientOne1, clientTwo);
    }

    @Test
    void testDefaultServiceConfig() {
        // Get a client for a service that doesn't have explicit configuration
        MicroserviceClient client = factory.getClient("service-three");

        // Verify the client is not null
        assertNotNull(client);

        // Verify the client is an instance of MicroserviceClient
        assertTrue(client instanceof MicroserviceClient);
    }

    @Test
    void testMultipleServices() {
        // Get clients for different services
        MicroserviceClient clientOne = factory.getClient("service-one");
        MicroserviceClient clientTwo = factory.getClient("service-two");

        // Verify the clients are not null
        assertNotNull(clientOne);
        assertNotNull(clientTwo);

        // Verify the clients are different instances
        assertNotSame(clientOne, clientTwo);
    }
}
