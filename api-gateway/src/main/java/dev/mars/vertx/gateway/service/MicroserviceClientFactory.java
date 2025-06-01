package dev.mars.vertx.gateway.service;

import dev.mars.vertx.common.resilience.CircuitBreakerFactory;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating and managing MicroserviceClient instances.
 * Ensures that only one client is created per service.
 */
public class MicroserviceClientFactory {
    private static final Logger logger = LoggerFactory.getLogger(MicroserviceClientFactory.class);

    private final Vertx vertx;
    private final JsonObject config;
    private final Map<String, MicroserviceClient> clients = new HashMap<>();

    /**
     * Creates a new factory.
     *
     * @param vertx the Vertx instance
     * @param config the configuration
     */
    public MicroserviceClientFactory(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        logger.info("Created MicroserviceClientFactory");
    }

    /**
     * Gets or creates a client for the specified service.
     *
     * @param serviceName the name of the service
     * @return the client
     */
    public MicroserviceClient getClient(String serviceName) {
        return clients.computeIfAbsent(serviceName, this::createClient);
    }

    /**
     * Creates a new client for the specified service.
     *
     * @param serviceName the name of the service
     * @return the new client
     */
    private MicroserviceClient createClient(String serviceName) {
        logger.info("Creating client for service: {}", serviceName);

        // Get service configuration
        JsonObject serviceConfig = config.getJsonObject("services", new JsonObject())
                .getJsonObject(serviceName, new JsonObject());

        // Get service address
        String serviceAddress = serviceConfig.getString("address", 
                config.getString("service." + serviceName + ".address", "service." + serviceName));

        // Create circuit breaker
        CircuitBreaker circuitBreaker = createCircuitBreaker(serviceName, serviceConfig);

        // Create and return client
        return new MicroserviceClient(vertx, circuitBreaker, serviceAddress);
    }

    /**
     * Creates a circuit breaker for the specified service.
     *
     * @param serviceName the name of the service
     * @param serviceConfig the service configuration
     * @return the circuit breaker
     */
    private CircuitBreaker createCircuitBreaker(String serviceName, JsonObject serviceConfig) {
        // Get circuit breaker configuration
        JsonObject cbConfig = serviceConfig.getJsonObject("circuit-breaker", new JsonObject());

        // Get circuit breaker parameters
        int maxFailures = cbConfig.getInteger("max-failures", 5);
        long timeout = cbConfig.getLong("timeout", 10000L);
        long resetTimeout = cbConfig.getLong("reset-timeout", 30000L);

        logger.info("Creating circuit breaker for service {} with options: maxFailures={}, timeout={}, resetTimeout={}",
                serviceName, maxFailures, timeout, resetTimeout);

        // Create and return circuit breaker
        return CircuitBreakerFactory.create(vertx, serviceName, maxFailures, timeout, resetTimeout);
    }
}
