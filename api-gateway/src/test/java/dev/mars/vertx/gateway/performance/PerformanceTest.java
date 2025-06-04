package dev.mars.vertx.gateway.performance;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the API Gateway.
 * These tests measure response times and throughput under load.
 */

@ExtendWith(VertxExtension.class)
class PerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceTest.class);
    private static final int TEST_PORT = 8888;
    private static final String TEST_HOST = "localhost";
    private static final String HEALTH_ENDPOINT = "/health";
    private static final int CONCURRENT_REQUESTS = 100;
    private static final int WARMUP_REQUESTS = 10;
    private static final long MAX_EXPECTED_AVG_RESPONSE_TIME_MS = 200;

    private Vertx vertx;
    private HttpClient client;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();

        // Configure HTTP client with connection pooling for performance testing
        HttpClientOptions options = new HttpClientOptions()
                .setMaxPoolSize(50)
                .setKeepAlive(true)
                .setPipelining(true)
                .setConnectTimeout(1000);

        client = vertx.createHttpClient(options);

        // Note: This test assumes the API Gateway is already running on TEST_PORT
        // In a real test environment, we would deploy the API Gateway here
        logger.info("Performance test setup complete. Assuming API Gateway is running on port {}", TEST_PORT);
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        client.close();
        vertx.close()
            .onComplete(testContext.succeeding(v -> {
                testContext.completeNow();
            }));
    }

    @Test
    void testResponseTimeUnderLoad(VertxTestContext testContext) {
        // First do some warmup requests
        logger.info("Starting warmup with {} requests", WARMUP_REQUESTS);
        List<Future<Long>> warmupFutures = new ArrayList<>();

        for (int i = 0; i < WARMUP_REQUESTS; i++) {
            warmupFutures.add(sendRequest());
        }

        // After warmup, perform the actual test
        Future.all(warmupFutures)
            .onComplete(ar -> {
                logger.info("Warmup complete, starting performance test with {} concurrent requests", CONCURRENT_REQUESTS);

                // Track completion and response times
                AtomicInteger completed = new AtomicInteger(0);
                List<Long> responseTimes = new ArrayList<>();

                // Start time for throughput calculation
                long startTime = System.currentTimeMillis();

                // Send concurrent requests
                for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
                    sendRequest()
                        .onComplete(result -> {
                            if (result.succeeded()) {
                                responseTimes.add(result.result());
                            }

                            // If all requests are completed, calculate metrics
                            if (completed.incrementAndGet() == CONCURRENT_REQUESTS) {
                                long endTime = System.currentTimeMillis();
                                long totalTime = endTime - startTime;

                                // Calculate metrics
                                double avgResponseTime = responseTimes.stream()
                                        .mapToLong(Long::longValue)
                                        .average()
                                        .orElse(0);

                                long minResponseTime = responseTimes.stream()
                                        .mapToLong(Long::longValue)
                                        .min()
                                        .orElse(0);

                                long maxResponseTime = responseTimes.stream()
                                        .mapToLong(Long::longValue)
                                        .max()
                                        .orElse(0);

                                double requestsPerSecond = (CONCURRENT_REQUESTS * 1000.0) / totalTime;

                                // Log performance metrics
                                logger.info("Performance test results:");
                                logger.info("Total requests: {}", CONCURRENT_REQUESTS);
                                logger.info("Total time: {} ms", totalTime);
                                logger.info("Average response time: {:.2f} ms", avgResponseTime);
                                logger.info("Min response time: {} ms", minResponseTime);
                                logger.info("Max response time: {} ms", maxResponseTime);
                                logger.info("Throughput: {:.2f} requests/second", requestsPerSecond);

                                // Verify performance meets expectations
                                testContext.verify(() -> {
                                    assertTrue(avgResponseTime < MAX_EXPECTED_AVG_RESPONSE_TIME_MS, 
                                            "Average response time should be less than " + MAX_EXPECTED_AVG_RESPONSE_TIME_MS + " ms");
                                    testContext.completeNow();
                                });
                            }
                        });
                }
            });

        // Set a timeout for the test
        try {
            assertTrue(testContext.awaitCompletion(30, TimeUnit.SECONDS), "Test timed out");
        } catch (InterruptedException e) {
            fail("Test was interrupted: " + e.getMessage());
        }
    }

    /**
     * Sends a request to the health endpoint and returns the response time in milliseconds.
     * 
     * @return Future with the response time in milliseconds
     */
    private Future<Long> sendRequest() {
        long startTime = System.currentTimeMillis();

        return client.request(HttpMethod.GET, TEST_PORT, TEST_HOST, HEALTH_ENDPOINT)
            .compose(request -> request.send())
            .compose(response -> {
                // Verify the response is successful
                assertEquals(200, response.statusCode());
                return response.body();
            })
            .map(body -> {
                // Verify the response body is valid JSON
                JsonObject json = new JsonObject(body);
                assertEquals("UP", json.getString("status"));

                // Calculate and return response time
                return System.currentTimeMillis() - startTime;
            });
    }
}
