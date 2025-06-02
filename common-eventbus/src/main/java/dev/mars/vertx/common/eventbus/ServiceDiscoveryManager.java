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