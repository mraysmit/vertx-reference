package dev.mars.vertx.service.one;

import dev.mars.vertx.common.eventbus.EventBusService;
import dev.mars.vertx.common.eventbus.ServiceDiscoveryManager;
import dev.mars.vertx.service.one.handler.ItemHandler;
import dev.mars.vertx.service.one.repository.InMemoryItemRepository;
import dev.mars.vertx.service.one.repository.ItemRepositoryInterface;
import dev.mars.vertx.service.one.service.ItemService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service One Verticle.
 * Handles requests from the API Gateway via the event bus.
 * Demonstrates best practices for Vert.x verticles with repository pattern.
 */
public class ServiceOneVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ServiceOneVerticle.class);

    private ServiceDiscoveryManager serviceDiscoveryManager;
    private EventBusService eventBusService;
    private MessageConsumer<JsonObject> consumer;
    private String defaultServiceAddress = "service.one";
    private String defaultServiceName = "Service One";

    // Components
    private ItemRepositoryInterface itemRepositoryInterface;
    private ItemService itemService;
    private ItemHandler itemHandler;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting Service One Verticle");

        // Initialize components
        initializeComponents()
            .compose(v -> registerEventBusConsumer())
            .onSuccess(v -> {
                logger.info("Service One Verticle started successfully");
                startPromise.complete();
            })
            .onFailure(err -> {
                logger.error("Failed to start Service One Verticle", err);
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
        itemRepositoryInterface = new InMemoryItemRepository();

        // Create service
        itemService = new ItemService(itemRepositoryInterface);

        // Create handler
        itemHandler = new ItemHandler(itemService);

        // Initialize event bus service
        eventBusService = new EventBusService(vertx);

        // Initialize service discovery manager
        serviceDiscoveryManager = new ServiceDiscoveryManager(vertx);

        // Initialize sample data
        return itemService.initialize();
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
                consumer = eventBusService.consumer(serviceAddress, itemHandler::handleRequest);
                return Future.succeededFuture();
        });
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        String serviceName = config().getString("service.name", defaultServiceName);
        String serviceAddress = config().getString("service.address", defaultServiceAddress);
        logger.info("Unregistering event bus consumer with service name: " + serviceAddress);

        logger.info("Stopping Service One Verticle");

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
            stopPromise.complete();
        }
    }
}
