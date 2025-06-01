package dev.mars.vertx.common.resilience;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating and configuring circuit breakers.
 * Circuit breakers help prevent cascading failures in distributed systems.
 */
public class CircuitBreakerFactory {
    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerFactory.class);

    /**
     * Creates a circuit breaker with default settings.
     * 
     * @param vertx the Vertx instance
     * @param name the name of the circuit breaker
     * @return the created circuit breaker
     */
    public static CircuitBreaker createDefault(Vertx vertx, String name) {
        logger.info("Creating circuit breaker with default settings: {}", name);
        logger.debug("Default settings: maxFailures=5, timeout=10000ms, resetTimeout=30000ms");
        return create(vertx, name, 5, 10000, 30000);
    }

    /**
     * Creates a circuit breaker with custom settings.
     * 
     * @param vertx the Vertx instance
     * @param name the name of the circuit breaker
     * @param maxFailures the maximum number of failures before opening the circuit
     * @param timeout the timeout in milliseconds
     * @param resetTimeout the time in milliseconds before attempting to reset the circuit
     * @return the created circuit breaker
     */
    public static CircuitBreaker create(Vertx vertx, String name, int maxFailures, long timeout, long resetTimeout) {
        logger.info("Creating circuit breaker: {} (maxFailures={}, timeout={}ms, resetTimeout={}ms)",
                name, maxFailures, timeout, resetTimeout);

        logger.debug("Configuring circuit breaker options for: {}", name);
        CircuitBreakerOptions options = new CircuitBreakerOptions()
                .setMaxFailures(maxFailures)
                .setTimeout(timeout)
                .setResetTimeout(resetTimeout)
                .setFallbackOnFailure(true)
                .setNotificationAddress("circuit-breaker." + name);

        logger.debug("Circuit breaker options configured");

        logger.debug("Creating circuit breaker instance: {}", name);
        CircuitBreaker breaker = CircuitBreaker.create(name, vertx, options);

        // Add listeners for state changes
        breaker.openHandler(v -> {
            logger.warn("Circuit breaker {} is now OPEN - requests will fail fast", name);
            logger.debug("Circuit will attempt to reset after {}ms", resetTimeout);
        });

        breaker.halfOpenHandler(v -> {
            logger.info("Circuit breaker {} is now HALF-OPEN - testing if service is available", name);
            logger.debug("Next request will determine if circuit returns to CLOSED or OPEN state");
        });

        breaker.closeHandler(v -> {
            logger.info("Circuit breaker {} is now CLOSED - service is operating normally", name);
        });


        logger.info("Circuit breaker {} created successfully", name);
        return breaker;
    }
}
