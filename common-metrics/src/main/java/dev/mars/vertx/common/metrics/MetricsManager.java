package dev.mars.vertx.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for setting up and managing metrics in Vert.x applications.
 * Uses Micrometer with Prometheus for metrics collection and exposure.
 */
public class MetricsManager {
    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);
    private static PrometheusMeterRegistry registry;

    /**
     * Configures Vert.x options with Micrometer metrics.
     *
     * @param options the Vert.x options to configure
     * @param enableJvmMetrics whether to enable JVM metrics
     * @return the configured Vert.x options
     */
    public static VertxOptions configureMetrics(VertxOptions options, boolean enableJvmMetrics) {
        logger.info("Configuring Vert.x metrics with Micrometer and Prometheus");
        logger.debug("Initial VertxOptions: {}", options);
        
        // Create Prometheus registry if not already created
        if (registry == null) {
            logger.debug("Creating new Prometheus registry");
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            
            // Add JVM metrics if enabled
            if (enableJvmMetrics) {
                logger.info("Enabling JVM metrics");
                new ClassLoaderMetrics().bindTo(registry);
                logger.debug("ClassLoader metrics enabled");
                
                new JvmMemoryMetrics().bindTo(registry);
                logger.debug("JVM Memory metrics enabled");
                
                new JvmGcMetrics().bindTo(registry);
                logger.debug("JVM GC metrics enabled");
                
                new JvmThreadMetrics().bindTo(registry);
                logger.debug("JVM Thread metrics enabled");
                
                new ProcessorMetrics().bindTo(registry);
                logger.debug("Processor metrics enabled");
                
                logger.info("All JVM metrics enabled successfully");
            } else {
                logger.info("JVM metrics not enabled");
            }
        } else {
            logger.debug("Using existing Prometheus registry");
        }
        
        // Configure Prometheus options
        VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
                .setEnabled(true)
                .setStartEmbeddedServer(true)
                .setEmbeddedServerOptions(new io.vertx.core.http.HttpServerOptions().setPort(9090))
                .setPublishQuantiles(true);
        
        logger.debug("Prometheus options configured: embedded server on port 9090, quantiles enabled");
        
        // Configure Micrometer options
        MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                .setEnabled(true)
                .setPrometheusOptions(prometheusOptions)
                .setMicrometerRegistry(registry);
        
        logger.debug("Micrometer metrics options configured");
        
        // Set metrics options on VertxOptions
        VertxOptions configuredOptions = options.setMetricsOptions(metricsOptions);
        logger.info("Vert.x metrics configuration completed successfully");
        
        return configuredOptions;
    }

    /**
     * Gets the Prometheus meter registry.
     *
     * @return the Prometheus meter registry
     */
    public static PrometheusMeterRegistry getRegistry() {
        if (registry == null) {
            logger.debug("Creating new Prometheus registry on first access");
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        } else {
            logger.trace("Returning existing Prometheus registry");
        }
        return registry;
    }

    /**
     * Creates a new Vertx instance with metrics enabled.
     *
     * @param enableJvmMetrics whether to enable JVM metrics
     * @return a new Vertx instance with metrics enabled
     */
    public static Vertx createVertxWithMetrics(boolean enableJvmMetrics) {
        logger.info("Creating new Vertx instance with metrics enabled (JVM metrics: {})", enableJvmMetrics);
        VertxOptions options = new VertxOptions();
        configureMetrics(options, enableJvmMetrics);
        Vertx vertx = Vertx.vertx(options);
        logger.debug("Vertx instance created with metrics enabled");
        return vertx;
    }
    
    /**
     * Registers custom metrics for an application component.
     *
     * @param componentName the name of the component
     * @return the meter registry for registering metrics
     */
    public static MeterRegistry registerComponentMetrics(String componentName) {
        logger.info("Registering metrics for component: {}", componentName);
        PrometheusMeterRegistry registry = getRegistry();
        // Add component tag to all metrics created from this point
        registry.config().commonTags("component", componentName);
        logger.debug("Component metrics registered with common tag: component={}", componentName);
        return registry;
    }
}