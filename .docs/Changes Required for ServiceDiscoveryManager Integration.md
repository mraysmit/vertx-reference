
# Changes Required for ServiceDiscoveryManager Integration

Based on my analysis of the codebase, here are the changes needed to integrate the new `ServiceDiscoveryManager` class into the service-two module and other relevant modules:

## 1. Changes to ServiceTwoVerticle.java

The `ServiceTwoVerticle` class needs to be updated to use the `ServiceDiscoveryManager` for service registration and discovery:

```java
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
```

## 2. No Changes Needed for config.yaml

The `config.yaml` file for service-two already contains the necessary configuration:

```yaml
service:
  name: Service Two
  address: service.two
```

These properties are used by the `ServiceTwoVerticle` to register with the `ServiceDiscoveryManager`.

## 3. No Changes Needed for VertxReferenceBootstrap.java

The `VertxReferenceBootstrap` class already deploys the `ServiceTwoVerticle` with the necessary configuration:

```java
private Future<String> deployServiceTwo() {
    logger.info("Deploying Service Two");

    DeploymentOptions options = new DeploymentOptions()
            .setConfig(new JsonObject()
                    .put("service.address", "service.two")
                    .put("http.port", 8082)
            );

    return vertx.deployVerticle(ServiceTwoVerticle.class.getName(), options);
}
```

## 4. No Changes Needed for ConfigLoader.java

The `ConfigLoader` class doesn't interact with the `ServiceDiscoveryManager`, so no changes are needed.

## 5. Import Dependencies

Make sure the service-two module has the necessary dependencies in its pom.xml file to use the `ServiceDiscoveryManager` class from the common-eventbus module.

## Summary of Changes

1. Added `ServiceDiscoveryManager` field to `ServiceTwoVerticle`
2. Initialized `ServiceDiscoveryManager` in the `initializeComponents` method
3. Updated `registerEventBusConsumer` method to register with `ServiceDiscoveryManager`
4. Updated `stop` method to unregister from `ServiceDiscoveryManager`

These changes align service-two with the pattern established in service-one for using the new `ServiceDiscoveryManager` class for service registration and discovery.