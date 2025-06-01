package dev.mars.vertx.common.metrics;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.micrometer.MicrometerMetricsOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class MetricsManagerTest {

    private Vertx vertx;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        // Create a Vertx instance without metrics for testing
        vertx = Vertx.vertx();
        testContext.completeNow();
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void testConfigureMetrics(VertxTestContext testContext) {
        // Test configuring metrics
        VertxOptions options = new VertxOptions();
        
        // Configure metrics
        VertxOptions configuredOptions = MetricsManager.configureMetrics(options, true);
        
        testContext.verify(() -> {
            // Verify that metrics are enabled
            assertTrue(configuredOptions.getMetricsOptions().isEnabled());
            
            // Verify that the metrics options is of type MicrometerMetricsOptions
            assertTrue(configuredOptions.getMetricsOptions() instanceof MicrometerMetricsOptions);
            
            // Verify that Prometheus is enabled
            MicrometerMetricsOptions metricsOptions = (MicrometerMetricsOptions) configuredOptions.getMetricsOptions();
            assertTrue(metricsOptions.getPrometheusOptions().isEnabled());
            
            testContext.completeNow();
        });
    }

    @Test
    void testGetRegistry(VertxTestContext testContext) {
        // Test getting the registry
        PrometheusMeterRegistry registry = MetricsManager.getRegistry();
        
        testContext.verify(() -> {
            // Verify that the registry is not null
            assertNotNull(registry);
            
            // Verify that getting the registry again returns the same instance
            assertSame(registry, MetricsManager.getRegistry());
            
            testContext.completeNow();
        });
    }

    @Test
    void testCreateVertxWithMetrics(VertxTestContext testContext) {
        // Test creating a Vertx instance with metrics
        Vertx metricsVertx = MetricsManager.createVertxWithMetrics(true);
        
        testContext.verify(() -> {
            // Verify that the Vertx instance is not null
            assertNotNull(metricsVertx);
            
            // Verify that metrics are enabled
            assertTrue(metricsVertx.isMetricsEnabled());
            
            // Clean up
            metricsVertx.close(testContext.succeedingThenComplete());
        });
    }
}