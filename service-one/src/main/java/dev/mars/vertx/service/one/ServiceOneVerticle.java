package dev.mars.vertx.service.one;

import dev.mars.vertx.common.eventbus.EventBusService;
import dev.mars.vertx.service.one.handler.ItemHandler;
import dev.mars.vertx.service.one.repository.InMemoryItemRepository;
import dev.mars.vertx.service.one.repository.ItemRepository;
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

    private EventBusService eventBusService;
    private MessageConsumer<JsonObject> consumer;

    // Components
    private ItemRepository itemRepository;
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
        itemRepository = new InMemoryItemRepository();

        // Create service
        itemService = new ItemService(itemRepository);

        // Create handler
        itemHandler = new ItemHandler(itemService);

        // Initialize event bus service
        eventBusService = new EventBusService(vertx);

        // Initialize sample data
        return itemService.initialize();
    }

    /**
     * Registers the event bus consumer.
     * 
     * @return a Future that completes when registration is done
     */
    private Future<Void> registerEventBusConsumer() {
        logger.info("Registering event bus consumer");

        String serviceAddress = config().getString("service.address", "service.one");
        consumer = eventBusService.consumer(serviceAddress, itemHandler::handleRequest);

        return Future.succeededFuture();
    }

    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("Stopping Service One Verticle");

        // Unregister event bus consumer
        if (consumer != null) {
            eventBusService.unregisterConsumer(consumer)
                .onSuccess(v -> {
                    logger.info("Event bus consumer unregistered successfully");
                    stopPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("Failed to unregister event bus consumer", err);
                    stopPromise.fail(err);
                });
        } else {
            stopPromise.complete();
        }
    }
}
