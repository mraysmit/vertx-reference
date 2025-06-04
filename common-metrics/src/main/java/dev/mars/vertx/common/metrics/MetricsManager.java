package dev.mars.vertx.common.metrics;

import dev.mars.vertx.common.config.ConfigLoader;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
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
     * @param vertx the Vertx instance
     * @param options the Vert.x options to configure
     * @param enableJvmMetrics whether to enable JVM metrics
     * @return a Future with the configured Vert.x options
     */
    public static Future<VertxOptions> configureMetricsAsync(Vertx vertx, VertxOptions options, boolean enableJvmMetrics) {
        logger.info("Configuring Vert.x metrics with Micrometer and Prometheus from configuration");

        return ConfigLoader.load(vertx, "common-metrics/src/main/resources/config.yaml")
            .map(config -> {
                logger.info("Loaded metrics configuration from config.yaml");
                return configureMetricsFromConfig(options, enableJvmMetrics, config);
            })
            .recover(err -> {
                logger.warn("Failed to load metrics configuration from config.yaml, using defaults", err);
                return Future.succeededFuture(configureMetrics(options, enableJvmMetrics));
            });
    }

    /**
     * Configures Vert.x options with Micrometer metrics using configuration.
     *
     * @param options the Vert.x options to configure
     * @param enableJvmMetrics whether to enable JVM metrics
     * @param config the configuration
     * @return the configured Vert.x options
     */
    private static VertxOptions configureMetricsFromConfig(VertxOptions options, boolean enableJvmMetrics, JsonObject config) {
        logger.debug("Initial VertxOptions: {}", options);

        // Get Prometheus configuration
        JsonObject prometheusConfig = config.getJsonObject("prometheus", new JsonObject());
        boolean prometheusEnabled = prometheusConfig.getBoolean("enabled", true);

        // Get embedded server configuration
        JsonObject embeddedServerConfig = prometheusConfig.getJsonObject("embedded-server", new JsonObject());
        boolean embeddedServerEnabled = embeddedServerConfig.getBoolean("enabled", true);
        int embeddedServerPort = embeddedServerConfig.getInteger("port", 9090);

        // Get JVM metrics configuration
        JsonObject jvmMetricsConfig = config.getJsonObject("jvm-metrics", new JsonObject());
        boolean jvmMetricsEnabled = jvmMetricsConfig.getBoolean("enabled", true) && enableJvmMetrics;

        // Create Prometheus registry if not already created
        if (registry == null) {
            logger.debug("Creating new Prometheus registry");
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

            // Add JVM metrics if enabled
            if (jvmMetricsEnabled) {
                logger.info("Enabling JVM metrics from configuration");
                JsonObject typesConfig = jvmMetricsConfig.getJsonObject("types", new JsonObject());

                if (typesConfig.getBoolean("class-loader", true)) {
                    new ClassLoaderMetrics().bindTo(registry);
                    logger.debug("ClassLoader metrics enabled");
                }

                if (typesConfig.getBoolean("memory", true)) {
                    new JvmMemoryMetrics().bindTo(registry);
                    logger.debug("JVM Memory metrics enabled");
                }

                if (typesConfig.getBoolean("gc", true)) {
                    new JvmGcMetrics().bindTo(registry);
                    logger.debug("JVM GC metrics enabled");
                }

                if (typesConfig.getBoolean("thread", true)) {
                    new JvmThreadMetrics().bindTo(registry);
                    logger.debug("JVM Thread metrics enabled");
                }

                if (typesConfig.getBoolean("processor", true)) {
                    new ProcessorMetrics().bindTo(registry);
                    logger.debug("Processor metrics enabled");
                }

                logger.info("JVM metrics enabled according to configuration");
            } else {
                logger.info("JVM metrics not enabled");
            }
        } else {
            logger.debug("Using existing Prometheus registry");
        }

        // Configure Prometheus options
        VertxPrometheusOptions prometheusOptions = new VertxPrometheusOptions()
                .setEnabled(prometheusEnabled)
                .setStartEmbeddedServer(embeddedServerEnabled)
                .setEmbeddedServerOptions(new io.vertx.core.http.HttpServerOptions().setPort(embeddedServerPort))
                .setPublishQuantiles(prometheusConfig.getBoolean("publish-quantiles", true));

        logger.debug("Prometheus options configured from config: enabled={}, embeddedServer={}, port={}, publishQuantiles={}",
                prometheusEnabled, embeddedServerEnabled, embeddedServerPort, prometheusConfig.getBoolean("publish-quantiles", true));

        // Configure Micrometer options
        MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions()
                .setEnabled(true)
                .setPrometheusOptions(prometheusOptions)
                .setMicrometerRegistry(registry);

        logger.debug("Micrometer metrics options configured");

        // Set metrics options on VertxOptions
        VertxOptions configuredOptions = options.setMetricsOptions(metricsOptions);
        logger.info("Vert.x metrics configuration completed successfully from config.yaml");

        return configuredOptions;
    }

    /**
     * Configures Vert.x options with Micrometer metrics using default values.
     *
     * @param options the Vert.x options to configure
     * @param enableJvmMetrics whether to enable JVM metrics
     * @return the configured Vert.x options
     * @deprecated Use {@link #configureMetricsAsync(Vertx, VertxOptions, boolean)} instead
     */
    @Deprecated
    public static VertxOptions configureMetrics(VertxOptions options, boolean enableJvmMetrics) {
        logger.info("Configuring Vert.x metrics with Micrometer and Prometheus using default values");
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
     * @deprecated Use {@link #createVertxWithMetricsAsync(boolean)} instead
     */
    @Deprecated
    public static Vertx createVertxWithMetrics(boolean enableJvmMetrics) {
        logger.info("Creating new Vertx instance with metrics enabled using default values (JVM metrics: {})", enableJvmMetrics);
        VertxOptions options = new VertxOptions();
        configureMetrics(options, enableJvmMetrics);
        Vertx vertx = Vertx.vertx(options);
        logger.debug("Vertx instance created with metrics enabled using default values");
        return vertx;
    }

    /**
     * Creates a new Vertx instance with metrics enabled using configuration.
     *
     * @param enableJvmMetrics whether to enable JVM metrics
     * @return a Future with a new Vertx instance with metrics enabled
     */
    public static Future<Vertx> createVertxWithMetricsAsync(boolean enableJvmMetrics) {
        logger.info("Creating new Vertx instance with metrics enabled from configuration (JVM metrics: {})", enableJvmMetrics);

        // Create Vertx instance with default options first
        VertxOptions options = new VertxOptions();
        Vertx vertx = Vertx.vertx(options);

        // Then configure metrics using configuration
        return configureMetricsAsync(vertx, options, enableJvmMetrics)
            .map(configuredOptions -> {
                // Close the temporary Vertx instance
                vertx.close();

                // Create a new Vertx instance with the configured options
                Vertx newVertx = Vertx.vertx(configuredOptions);
                logger.debug("Vertx instance created with metrics enabled from configuration");
                return newVertx;
            })
            .recover(err -> {
                // Close the temporary Vertx instance on error
                vertx.close();

                // Return the error
                logger.error("Failed to create Vertx instance with metrics enabled from configuration", err);
                return Future.failedFuture(err);
            });
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
