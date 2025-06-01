package dev.mars.vertx.gateway.resilience;

import dev.mars.vertx.common.resilience.CircuitBreakerFactory;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerState;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for circuit breaker behavior.
 * Tests the circuit breaker's open, half-open, and closed states, as well as state transitions.
 */
@ExtendWith(VertxExtension.class)
class CircuitBreakerTest {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerTest.class);

    private Vertx vertx;
    private CircuitBreaker circuitBreaker;

    // Circuit breaker configuration
    private static final int MAX_FAILURES = 3;
    private static final long TIMEOUT = 1000L;
    private static final long RESET_TIMEOUT = 2000L;

    // Test service
    private TestService testService;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();

        // Create a circuit breaker with a short reset timeout for testing
        circuitBreaker = CircuitBreakerFactory.create(
                vertx, 
                "test-circuit-breaker", 
                MAX_FAILURES, 
                TIMEOUT, 
                RESET_TIMEOUT);

        // Create a test service
        testService = new TestService();

        logger.info("Circuit breaker test setup complete");
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close()
                .onComplete(testContext.succeeding(v -> {
                    testContext.completeNow();
                }));
    }

    @Test
    void testCircuitBreakerClosedState(VertxTestContext testContext) {
        // Configure the test service to succeed
        testService.setFailureMode(false);

        // Execute a request through the circuit breaker
        circuitBreaker.<String>execute(promise -> {
            testService.doOperation("test-request")
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            promise.complete(ar.result());
                        } else {
                            promise.fail(ar.cause());
                        }
                    });
        })
        .onComplete(testContext.succeeding(result -> {
            testContext.verify(() -> {
                // Verify the circuit breaker is in the closed state
                assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.state());

                // Verify the result
                assertEquals("test-request-processed", result);

                // Verify the failure count
                assertEquals(0, circuitBreaker.failureCount());

                testContext.completeNow();
            });
        }));
    }

    @Test
    void testCircuitBreakerOpenState(VertxTestContext testContext) {
        // Configure the test service to fail
        testService.setFailureMode(true);

        // Execute multiple requests to trigger the circuit breaker
        List<Future<String>> futures = new ArrayList<>();

        // Send MAX_FAILURES + 1 requests to open the circuit breaker
        for (int i = 0; i < MAX_FAILURES + 1; i++) {
            final int index = i;
            futures.add(circuitBreaker.<String>execute(promise -> {
                testService.doOperation("test-request-" + index)
                    .onComplete(ar -> {
                        if (ar.succeeded()) {
                            promise.complete(ar.result());
                        } else {
                            promise.fail(ar.cause());
                        }
                    });
            }));
        }

        // Wait for all requests to complete
        Future.join(futures)
                .onComplete(ar -> {
                    testContext.verify(() -> {
                        // Verify the circuit breaker is in the open state
                        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.state());

                        // Verify the failure count
                        assertEquals(MAX_FAILURES, circuitBreaker.failureCount());

                        // Try one more request, which should fail fast
                        circuitBreaker.<String>execute(promise -> {
                            testService.doOperation("test-request-fast-fail")
                                .onComplete(result -> {
                                    if (result.succeeded()) {
                                        promise.complete(result.result());
                                    } else {
                                        promise.fail(result.cause());
                                    }
                                });
                        })
                        .onComplete(testContext.failing(error -> {
                            testContext.verify(() -> {
                                // Verify the error message indicates circuit is open
                                assertTrue(error.getMessage().contains("open"), 
                                        "Error message should indicate circuit is open");

                                // Verify the test service was not called
                                assertFalse(testService.wasLastOperationCalled(), 
                                        "Service should not be called when circuit is open");

                                testContext.completeNow();
                            });
                        }));
                    });
                });
    }

    @Test
    void testCircuitBreakerHalfOpenState(VertxTestContext testContext) throws InterruptedException {
        // Configure the test service to fail
        testService.setFailureMode(true);

        // Execute multiple requests to trigger the circuit breaker
        List<Future<String>> futures = new ArrayList<>();

        // Send MAX_FAILURES + 1 requests to open the circuit breaker
        for (int i = 0; i < MAX_FAILURES + 1; i++) {
            final int index = i;
            futures.add(circuitBreaker.<String>execute(promise -> {
                testService.doOperation("test-request-" + index)
                    .onComplete(result -> {
                        if (result.succeeded()) {
                            promise.complete(result.result());
                        } else {
                            promise.fail(result.cause());
                        }
                    });
            }));
        }

        // Wait for all requests to complete
        Future.join(futures)
                .onComplete(ar -> {
                    // Verify the circuit breaker is in the open state
                    assertEquals(CircuitBreakerState.OPEN, circuitBreaker.state());

                    // Wait for the reset timeout to transition to half-open
                    vertx.setTimer(RESET_TIMEOUT + 500, id -> {
                        testContext.verify(() -> {
                            // Verify the circuit breaker is in the half-open state
                            assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.state());

                            // Configure the test service to succeed
                            testService.setFailureMode(false);

                            // Execute a request, which should succeed and close the circuit
                            circuitBreaker.<String>execute(promise -> {
                                testService.doOperation("test-request-half-open")
                                    .onComplete(result -> {
                                        if (result.succeeded()) {
                                            promise.complete(result.result());
                                        } else {
                                            promise.fail(result.cause());
                                        }
                                    });
                            })
                            .onComplete(testContext.succeeding(result -> {
                                testContext.verify(() -> {
                                    // Verify the circuit breaker is in the closed state
                                    assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.state());

                                    // Verify the result
                                    assertEquals("test-request-half-open-processed", result);

                                    testContext.completeNow();
                                });
                            }));
                        });
                    });
                });

        // Set a timeout for the test
        assertTrue(testContext.awaitCompletion(RESET_TIMEOUT + 2000, TimeUnit.MILLISECONDS), 
                "Test timed out");
    }

    @Test
    void testCircuitBreakerStateTransitions(VertxTestContext testContext) throws InterruptedException {
        // Track state transitions
        AtomicBoolean openHandlerCalled = new AtomicBoolean(false);
        AtomicBoolean halfOpenHandlerCalled = new AtomicBoolean(false);
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);

        // Register state transition handlers
        circuitBreaker.openHandler(v -> {
            logger.info("Circuit breaker opened");
            openHandlerCalled.set(true);
        });

        circuitBreaker.halfOpenHandler(v -> {
            logger.info("Circuit breaker half-opened");
            halfOpenHandlerCalled.set(true);
        });

        circuitBreaker.closeHandler(v -> {
            logger.info("Circuit breaker closed");
            closeHandlerCalled.set(true);
        });

        // Configure the test service to fail
        testService.setFailureMode(true);

        // Execute multiple requests to trigger the circuit breaker
        List<Future<String>> futures = new ArrayList<>();

        // Send MAX_FAILURES + 1 requests to open the circuit breaker
        for (int i = 0; i < MAX_FAILURES + 1; i++) {
            final int index = i;
            futures.add(circuitBreaker.<String>execute(promise -> {
                testService.doOperation("test-request-" + index)
                    .onComplete(result -> {
                        if (result.succeeded()) {
                            promise.complete(result.result());
                        } else {
                            promise.fail(result.cause());
                        }
                    });
            }));
        }

        // Wait for all requests to complete
        Future.join(futures)
                .onComplete(ar -> {
                    // Verify the open handler was called
                    assertTrue(openHandlerCalled.get(), "Open handler should be called");

                    // Wait for the reset timeout to transition to half-open
                    vertx.setTimer(RESET_TIMEOUT + 500, id -> {
                        // Verify the half-open handler was called
                        assertTrue(halfOpenHandlerCalled.get(), "Half-open handler should be called");

                        // Configure the test service to succeed
                        testService.setFailureMode(false);

                        // Execute a request, which should succeed and close the circuit
                        circuitBreaker.<String>execute(promise -> {
                            testService.doOperation("test-request-half-open")
                                .onComplete(result -> {
                                    if (result.succeeded()) {
                                        promise.complete(result.result());
                                    } else {
                                        promise.fail(result.cause());
                                    }
                                });
                        })
                        .onComplete(testContext.succeeding(result -> {
                            testContext.verify(() -> {
                                // Verify the close handler was called
                                assertTrue(closeHandlerCalled.get(), "Close handler should be called");

                                testContext.completeNow();
                            });
                        }));
                    });
                });

        // Set a timeout for the test
        assertTrue(testContext.awaitCompletion(RESET_TIMEOUT + 2000, TimeUnit.MILLISECONDS), 
                "Test timed out");
    }

    /**
     * A test service that can be configured to fail or succeed.
     */
    private static class TestService {
        private boolean failureMode = false;
        private final AtomicInteger operationCounter = new AtomicInteger(0);
        private boolean lastOperationCalled = false;

        /**
         * Sets the failure mode.
         *
         * @param failureMode true to make operations fail, false to make them succeed
         */
        public void setFailureMode(boolean failureMode) {
            this.failureMode = failureMode;
        }

        /**
         * Checks if the last operation was called.
         *
         * @return true if the last operation was called, false otherwise
         */
        public boolean wasLastOperationCalled() {
            boolean result = lastOperationCalled;
            lastOperationCalled = false;
            return result;
        }

        /**
         * Performs an operation that can succeed or fail based on the failure mode.
         *
         * @param request the request
         * @return a Future with the result or error
         */
        public Future<String> doOperation(String request) {
            lastOperationCalled = true;
            operationCounter.incrementAndGet();

            if (failureMode) {
                return Future.failedFuture("Operation failed");
            } else {
                return Future.succeededFuture(request + "-processed");
            }
        }
    }
}
