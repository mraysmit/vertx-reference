# Implementing Vert.x Service Discovery

## Current Architecture Analysis

After examining the project structure, I've identified how services currently communicate:

1. **Current Communication Mechanism**: Services use the Vert.x EventBus for communication
   - `EventBusService` in the common-eventbus module provides a wrapper around the EventBus
   - Services register consumers on specific addresses (e.g., "service.one", "service.two")
   - The API Gateway uses `MicroserviceClient` to send requests to services via the EventBus

2. **Current Service Registration**: 
   - Service addresses are hardcoded in configuration
   - The API Gateway is configured with these addresses
   - No dynamic service discovery mechanism exists

3. **Limitations of Current Approach**:
   - Service addresses are static and hardcoded
   - No support for dynamic service registration/discovery
   - Configuration changes required if service addresses change

## Implementation Plan

### 1. Add Vert.x Service Discovery Dependency

Add the Vert.x service discovery dependency to the common-eventbus module's pom.xml:

```xml
<!-- Vert.x Service Discovery -->
<dependency>
    <groupId>io.vertx</groupId>
    <artifactId>vertx-service-discovery</artifactId>
    <version>${vertx.version}</version>
</dependency>
```

### 2. Create Service Discovery Implementation

Create a new class `ServiceDiscoveryManager` in the common-eventbus module:

```java
package dev.mars.vertx.common.eventbus;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import io.vertx.servicediscovery.types.EventBusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages service discovery for the application.
 * Provides methods for registering and discovering services.
 */
public class ServiceDiscoveryManager {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscoveryManager.class);
    
    private final Vertx vertx;
    private final ServiceDiscovery discovery;
    private final Map<String, Record> publishedRecords = new HashMap<>();
    
    /**
     * Creates a new ServiceDiscoveryManager.
     * 
     * @param vertx the Vertx instance
     */
    public ServiceDiscoveryManager(Vertx vertx) {
        this(vertx, new ServiceDiscoveryOptions());
    }
    
    /**
     * Creates a new ServiceDiscoveryManager with custom options.
     * 
     * @param vertx the Vertx instance
     * @param options the service discovery options
     */
    public ServiceDiscoveryManager(Vertx vertx, ServiceDiscoveryOptions options) {
        this.vertx = vertx;
        this.discovery = ServiceDiscovery.create(vertx, options);
        logger.info("ServiceDiscoveryManager initialized");
    }
    
    /**
     * Registers a service with the service discovery.
     * 
     * @param name the name of the service
     * @param address the event bus address of the service
     * @param metadata additional metadata for the service
     * @return a Future with the published record
     */
    public Future<Record> registerService(String name, String address, JsonObject metadata) {
        logger.info("Registering service: {} at address: {}", name, address);
        
        Record record = EventBusService.createRecord(
            name,
            address,
            Object.class.getName(),
            metadata
        );
        
        return discovery.publish(record)
            .onSuccess(publishedRecord -> {
                publishedRecords.put(name, publishedRecord);
                logger.info("Service registered successfully: {}", name);
            })
            .onFailure(err -> 
                logger.error("Failed to register service: {}", name, err)
            );
    }
    
    /**
     * Discovers a service by name.
     * 
     * @param name the name of the service to discover
     * @return a Future with the event bus address of the service
     */
    public Future<String> discoverService(String name) {
        logger.info("Discovering service: {}", name);
        
        return discovery.getRecord(record -> record.getName().equals(name))
            .map(record -> {
                if (record != null) {
                    String address = record.getLocation().getString("endpoint");
                    logger.info("Service discovered: {} at address: {}", name, address);
                    return address;
                } else {
                    logger.warn("Service not found: {}", name);
                    throw new IllegalStateException("Service not found: " + name);
                }
            });
    }
    
    /**
     * Unregisters a service from the service discovery.
     * 
     * @param name the name of the service to unregister
     * @return a Future that completes when the service is unregistered
     */
    public Future<Void> unregisterService(String name) {
        logger.info("Unregistering service: {}", name);
        
        Record record = publishedRecords.get(name);
        if (record != null) {
            return discovery.unpublish(record.getRegistration())
                .onSuccess(v -> {
                    publishedRecords.remove(name);
                    logger.info("Service unregistered successfully: {}", name);
                })
                .onFailure(err -> 
                    logger.error("Failed to unregister service: {}", name, err)
                );
        } else {
            logger.warn("Service not found for unregistration: {}", name);
            return Future.succeededFuture();
        }
    }
    
    /**
     * Closes the service discovery.
     * 
     * @return a Future that completes when the service discovery is closed
     */
    public Future<Void> close() {
        logger.info("Closing ServiceDiscoveryManager");
        
        return Future.all(
            publishedRecords.keySet().stream()
                .map(this::unregisterService)
                .toList()
        ).mapEmpty()
        .compose(v -> {
            discovery.close();
            return Future.succeededFuture();
        });
    }
    
    /**
     * Gets the ServiceDiscovery instance.
     * 
     * @return the ServiceDiscovery instance
     */
    public ServiceDiscovery getDiscovery() {
        return discovery;
    }
}
```

### 3. Update ServiceOneVerticle to Register with Service Discovery

Modify the `ServiceOneVerticle` class to register with the service discovery:

```java
// Add to class fields
private ServiceDiscoveryManager serviceDiscoveryManager;

// In the initializeComponents method
serviceDiscoveryManager = new ServiceDiscoveryManager(vertx);

// In the registerEventBusConsumer method
String serviceAddress = config().getString("service.address", "service.one");
String serviceName = "service-one";

// Register with service discovery
return serviceDiscoveryManager.registerService(
    serviceName,
    serviceAddress,
    new JsonObject()
        .put("type", "eventbus")
        .put("service", serviceName)
)
.compose(record -> {
    consumer = eventBusService.consumer(serviceAddress, itemHandler::handleRequest);
    return Future.succeededFuture();
});

// In the stop method
if (serviceDiscoveryManager != null) {
    serviceDiscoveryManager.unregisterService("service-one")
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
    // Original code for unregistering consumer
}
```

### 4. Update ServiceTwoVerticle Similarly

Apply similar changes to `ServiceTwoVerticle` to register with the service discovery.

### 5. Update MicroserviceClientFactory to Use Service Discovery

Modify the `MicroserviceClientFactory` class to discover services:

```java
// Add to class fields
private ServiceDiscoveryManager serviceDiscoveryManager;

// In the constructor
this.serviceDiscoveryManager = new ServiceDiscoveryManager(vertx);

// Modify the createClient method
private MicroserviceClient createClient(String serviceName) {
    logger.info("Creating client for service: {}", serviceName);
    
    // Create circuit breaker
    CircuitBreaker circuitBreaker = createCircuitBreaker(serviceName, 
        config.getJsonObject("services", new JsonObject())
              .getJsonObject(serviceName, new JsonObject()));
    
    // Discover service address
    return serviceDiscoveryManager.discoverService(serviceName)
        .map(serviceAddress -> {
            logger.info("Creating client with discovered address: {}", serviceAddress);
            return new MicroserviceClient(vertx, circuitBreaker, serviceAddress);
        })
        .onFailure(err -> {
            logger.warn("Service discovery failed for {}, falling back to configuration", serviceName);
        })
        .recover(err -> {
            // Fallback to configuration if discovery fails
            String serviceAddress = config.getJsonObject("services", new JsonObject())
                    .getJsonObject(serviceName, new JsonObject())
                    .getString("address", "service." + serviceName);
            
            logger.info("Creating client with fallback address: {}", serviceAddress);
            return Future.succeededFuture(new MicroserviceClient(vertx, circuitBreaker, serviceAddress));
        })
        .result();
}
```

### 6. Update VertxReferenceBootstrap

Modify the `VertxReferenceBootstrap` class to ensure services are deployed in the correct order:

```java
// In the start method
// Deploy services in sequence
deployServiceOne()
    .compose(id -> deployServiceTwo())
    .compose(id -> deployApiGateway())
    .onComplete(ar -> {
        if (ar.succeeded()) {
            logger.info("All services deployed successfully");
            latch.countDown();
        } else {
            logger.error("Failed to deploy services", ar.cause());
        }
    });
```

## Testing the Implementation

1. Start the application using the bootstrap:
   ```bash
   java -jar vertx-reference-bootstrap/target/vertx-reference-bootstrap-1.0-SNAPSHOT-fat.jar
   ```

2. Verify that services register with the service discovery by checking the logs.

3. Verify that the API Gateway can discover services by checking the logs and testing the endpoints.

## Documentation

### Service Discovery in Vert.x Reference Project

The Vert.x Reference Project now uses Vert.x Service Discovery for dynamic service registration and discovery. This enables services to be discovered at runtime without hardcoded addresses.

#### How It Works

1. **Service Registration**:
   - When a service starts, it registers itself with the service discovery
   - Registration includes the service name, event bus address, and metadata

2. **Service Discovery**:
   - The API Gateway discovers services by name
   - If discovery fails, it falls back to configuration

3. **Service Unregistration**:
   - When a service stops, it unregisters itself from the service discovery

#### Benefits

- **Dynamic Service Discovery**: Services can be discovered at runtime
- **Resilience**: Fallback to configuration if discovery fails
- **Flexibility**: Services can change addresses without reconfiguring clients
- **Scalability**: Multiple instances of the same service can be registered and discovered

#### Usage

To register a service:
```java
serviceDiscoveryManager.registerService(
    "service-name",
    "service.address",
    new JsonObject().put("type", "eventbus")
);
```

To discover a service:
```java
serviceDiscoveryManager.discoverService("service-name")
    .onSuccess(address -> {
        // Use the discovered address
    });
```