package dev.mars.vertx.service.two;

import dev.mars.vertx.common.eventbus.EventBusService;
import dev.mars.vertx.common.eventbus.ServiceDiscoveryManager;
import dev.mars.vertx.service.two.handler.WeatherHandler;
import dev.mars.vertx.service.two.handler.WeatherHandlerInterface;
import dev.mars.vertx.service.two.repository.InMemoryWeatherRepository;
import dev.mars.vertx.service.two.repository.WeatherRepository;
import dev.mars.vertx.service.two.service.WeatherService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service Two Verticle.
 * Handles requests from the API Gateway via the event bus.
 * Demonstrates best practices for Vert.x verticles with repository pattern.
 * This service simulates a weather data service.
 */
public class ServiceTwoVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTwoVerticle.class);

    private ServiceDiscoveryManager serviceDiscoveryManager;
    private EventBusService eventBusService;
    private MessageConsumer<JsonObject> consumer;
    private String defaultServiceAddress = "service.two";
    private String defaultServiceName = "Service Two";

    // Components
    private WeatherRepository weatherRepository;
    private WeatherService weatherService;
    private WeatherHandlerInterface weatherHandler;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting Service Two Verticle");

        // Initialize components
        initializeComponents()
                .compose(v -> registerEventBusConsumer())
                .onSuccess(v -> {
                    logger.info("Service Two Verticle started successfully");
                    startPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("Failed to start Service Two Verticle", err);
                    startPromise.fail(err);
                });
    }

    /**
     * Initializes the components (repository, service, handler).
     *
     * @return a Future that completes when initialization is done
     */
    private Future<Void> initializeComponents() {
        logger.info("Initializing components");

        // Create repository
        weatherRepository = new InMemoryWeatherRepository();

        // Create service
        weatherService = new WeatherService(weatherRepository);

        // Create handler
        weatherHandler = new WeatherHandler(weatherService);

        // Initialize event bus service
        eventBusService = new EventBusService(vertx);

        // Initialize service discovery manager
        serviceDiscoveryManager = new ServiceDiscoveryManager(vertx);

        // Initialize sample data
        return weatherService.initialize();
    }

    /**
     * Registers the event bus consumer.
     *
     * @return a Future that completes when registration is done
     */
    private Future<Void> registerEventBusConsumer() {
        String serviceName = config().getString("service.name", defaultServiceName);
        String serviceAddress = config().getString("service.address", defaultServiceAddress);
        logger.info("Registering event bus consumer with service name: " + serviceAddress);

        // Register with service discovery
        return serviceDiscoveryManager
                .registerService(
                        serviceName,
                        serviceAddress,
                        new JsonObject()
                                .put("type", "eventbus")
                                .put("service", serviceName)
                )
                .compose(record -> {
                    consumer = eventBusService.consumer(serviceAddress, weatherHandler::handleRequest);
                    return Future.succeededFuture();
                });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        String serviceName = config().getString("service.name", defaultServiceName);
        String serviceAddress = config().getString("service.address", defaultServiceAddress);
        logger.info("Unregistering event bus consumer with service name: " + serviceAddress);

        logger.info("Stopping Service Two Verticle");

        if (serviceDiscoveryManager != null) {
            serviceDiscoveryManager.unregisterService(serviceName)
                    .compose(v -> {
                        if (consumer != null) {
                            return eventBusService.unregisterConsumer(consumer);
                        } else {
                            return Future.succeededFuture();
                        }
                    })
                    .onSuccess(v -> {
                        logger.info("Service unregistered and consumer unregistered successfully");
                        stopPromise.complete();
                    })
                    .onFailure(err -> {
                        logger.error("Failed to unregister service or consumer", err);
                        stopPromise.fail(err);
                    });
        } else if (consumer != null) {
            stopPromise.complete();
        }
    }
}