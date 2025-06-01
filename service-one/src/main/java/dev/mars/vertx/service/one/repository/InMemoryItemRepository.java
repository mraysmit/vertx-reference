package dev.mars.vertx.service.one.repository;

import dev.mars.vertx.service.one.model.Item;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory implementation of the ItemRepositoryInterface interface.
 */
public class InMemoryItemRepository implements ItemRepositoryInterface {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryItemRepository.class);

    private final Map<String, Item> items = new HashMap<>();

    /**
     * Constructor.
     */
    public InMemoryItemRepository() {
        // No initialization needed
    }

    @Override
    public Future<Item> findById(String id) {
        logger.debug("Finding item by ID: {}", id);

        Promise<Item> promise = Promise.promise();
        Item item = items.get(id);
        if (item != null) {
            logger.debug("Found item: {}", item);
            promise.complete(item);
        } else {
            logger.debug("Item not found with ID: {}", id);
            promise.fail("Item not found with ID: " + id);
        }
        return promise.future();
    }

    @Override
    public Future<List<Item>> findAll() {
        logger.debug("Finding all items");

        List<Item> itemList = new ArrayList<>(items.values());
        logger.debug("Found {} items", itemList.size());
        return Future.succeededFuture(itemList);
    }

    @Override
    public Future<Item> save(Item item) {
        logger.debug("Saving item: {}", item);

        Promise<Item> promise = Promise.promise();

        // If no ID, generate one (create operation)
        if (item.getId() == null || item.getId().isEmpty()) {
            item.setId(UUID.randomUUID().toString());
            item.setCreatedAt(System.currentTimeMillis());
            logger.debug("Created new item with ID: {}", item.getId());
        } else {
            // Update operation - check if item exists
            if (!items.containsKey(item.getId())) {
                logger.debug("Item not found for update with ID: {}", item.getId());
                return Future.failedFuture("Item not found with ID: " + item.getId());
            }

            // Preserve creation timestamp
            Item existingItem = items.get(item.getId());
            item.setCreatedAt(existingItem.getCreatedAt());
            item.setUpdatedAt(System.currentTimeMillis());
            logger.debug("Updated existing item with ID: {}", item.getId());
        }

        items.put(item.getId(), item);
        return Future.succeededFuture(item);
    }

    @Override
    public Future<Boolean> deleteById(String id) {
        logger.debug("Deleting item with ID: {}", id);

        Item removed = items.remove(id);
        boolean success = removed != null;

        if (success) {
            logger.debug("Successfully deleted item with ID: {}", id);
        } else {
            logger.debug("Item not found for deletion with ID: {}", id);
        }

        return Future.succeededFuture(success);
    }

    @Override
    public Future<Void> initialize() {
        logger.info("Initializing repository with sample data");

        Promise<Void> promise = Promise.promise();

        // Clear any existing items
        items.clear();

        // Create sample items
        for (int i = 1; i <= 5; i++) {
            String id = "item-" + i;
            Item item = new Item(
                id,
                "Sample Item " + i,
                "This is a sample item " + i,
                System.currentTimeMillis(),
                null
            );

            items.put(id, item);
        }

        logger.info("Sample data initialized with {} items", items.size());
        promise.complete();

        return promise.future();
    }

    @Override
    public Future<Integer> count() {
        logger.debug("Getting item count");

        return Future.succeededFuture(items.size());
    }
}
