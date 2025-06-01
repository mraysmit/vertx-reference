package dev.mars.vertx.common.resilience;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class CircuitBreakerFactoryTest {

    private Vertx vertx;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        testContext.completeNow();
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void testCreateDefault(VertxTestContext testContext) {
        // Test creating a circuit breaker with default settings
        CircuitBreaker breaker = CircuitBreakerFactory.createDefault(vertx, "test-breaker");
        
        testContext.verify(() -> {
            // Verify that the circuit breaker is not null
            assertNotNull(breaker);
            
            // Verify that the circuit breaker has the correct name
            assertEquals("test-breaker", breaker.name());
            
            // Verify that the circuit breaker is closed initially
            assertEquals("CLOSED", breaker.state().name());
            
            testContext.completeNow();
        });
    }

    @Test
    void testCreateWithCustomSettings(VertxTestContext testContext) {
        // Test creating a circuit breaker with custom settings
        int maxFailures = 3;
        long timeout = 5000;
        long resetTimeout = 15000;
        
        CircuitBreaker breaker = CircuitBreakerFactory.create(
                vertx, "custom-breaker", maxFailures, timeout, resetTimeout);
        
        testContext.verify(() -> {
            // Verify that the circuit breaker is not null
            assertNotNull(breaker);
            
            // Verify that the circuit breaker has the correct name
            assertEquals("custom-breaker", breaker.name());
            
            // Verify that the circuit breaker is closed initially
            assertEquals("CLOSED", breaker.state().name());
            
            testContext.completeNow();
        });
    }

    @Test
    void testCircuitBreakerOpens(VertxTestContext testContext) throws InterruptedException {
        // Test that the circuit breaker opens after max failures
        int maxFailures = 3;
        CircuitBreaker breaker = CircuitBreakerFactory.create(
                vertx, "failing-breaker", maxFailures, 1000, 5000);
        
        // Counter for state changes
        AtomicInteger openStateCounter = new AtomicInteger(0);
        
        // Add a listener for state changes
        breaker.openHandler(v -> {
            openStateCounter.incrementAndGet();
            testContext.verify(() -> {
                assertEquals("OPEN", breaker.state().name());
                testContext.completeNow();
            });
        });
        
        // Execute failing operations to trigger the circuit breaker
        for (int i = 0; i < maxFailures + 1; i++) {
            breaker.execute(promise -> {
                promise.fail("Deliberate failure for testing");
            });
        }
        
        // Wait for the circuit breaker to open
        assertTrue(testContext.awaitCompletion(5, TimeUnit.SECONDS));
        assertEquals(1, openStateCounter.get());
    }

    @Test
    void testCircuitBreakerFallback(VertxTestContext testContext) {
        // Test that the circuit breaker uses fallback when specified
        CircuitBreaker breaker = CircuitBreakerFactory.createDefault(vertx, "fallback-breaker");
        
        // Execute an operation with a fallback
        breaker.executeWithFallback(
            promise -> {
                // This will fail
                promise.fail("Deliberate failure for testing");
            },
            error -> {
                // This is the fallback
                return "Fallback value";
            }
        ).onComplete(testContext.succeeding(result -> {
            testContext.verify(() -> {
                assertEquals("Fallback value", result);
                testContext.completeNow();
            });
        }));
    }

    @Test
    void testCircuitBreakerSuccess(VertxTestContext testContext) {
        // Test that the circuit breaker passes through successful operations
        CircuitBreaker breaker = CircuitBreakerFactory.createDefault(vertx, "success-breaker");
        
        // Execute a successful operation
        breaker.execute(promise -> {
            promise.complete("Success value");
        }).onComplete(testContext.succeeding(result -> {
            testContext.verify(() -> {
                assertEquals("Success value", result);
                testContext.completeNow();
            });
        }));
    }
}