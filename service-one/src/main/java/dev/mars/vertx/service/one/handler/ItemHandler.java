package dev.mars.vertx.service.one.handler;

import dev.mars.vertx.service.one.model.Item;
import dev.mars.vertx.service.one.service.ItemService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for item-related requests from the event bus.
 * Implements the ItemHandlerInterface for CRUD operations.
 */
public class ItemHandler implements ItemHandlerInterface {
    private static final Logger logger = LoggerFactory.getLogger(ItemHandler.class);

    private final ItemService itemService;

    /**
     * Constructor.
     *
     * @param itemService the item service
     */
    public ItemHandler(ItemService itemService) {
        this.itemService = itemService;
    }

    /**
     * Handles a request from the event bus.
     *
     * @param request the request
     * @return a Future with the response
     */
    public Future<Object> handleRequest(JsonObject request) {
        logger.info("Handling request: {}", request);

        try {
            // Check if the request has an action
            if (request.containsKey("action")) {
                String action = request.getString("action");

                switch (action) {
                    case "create":
                        return createItem(request).map(item -> (Object) item);
                    case "update":
                        return updateItem(request).map(item -> (Object) item);
                    case "delete":
                        return deleteItem(request).map(result -> (Object) result);
                    case "list":
                        return listItems().map(result -> (Object) result);
                    default:
                        return Future.failedFuture("Unknown action: " + action);
                }
            } else if (request.containsKey("id")) {
                String id = request.getString("id");
                return getItem(id).map(item -> (Object) item);
            } else {
                return Future.failedFuture("Invalid request: missing id or action");
            }
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return Future.failedFuture("Error processing request: " + e.getMessage());
        }
    }

    /**
     * Gets an item by ID.
     *
     * @param id the item ID
     * @return a Future with the item as a JsonObject
     */
    @Override
    public Future<JsonObject> getItem(String id) {
        logger.debug("Getting item with ID: {}", id);
        return itemService.getItem(id).map(Item::toJson);
    }

    /**
     * Creates a new item.
     *
     * @param request the request containing the item data
     * @return a Future with the created item as a JsonObject
     */
    @Override
    public Future<JsonObject> createItem(JsonObject request) {
        logger.debug("Creating new item: {}", request);
        return itemService.createItem(request).map(Item::toJson);
    }

    /**
     * Updates an existing item.
     *
     * @param request the request containing the item data
     * @return a Future with the updated item as a JsonObject
     */
    @Override
    public Future<JsonObject> updateItem(JsonObject request) {
        logger.debug("Updating item: {}", request);

        String id = request.getString("id");
        if (id == null) {
            return Future.failedFuture("ID is required for update");
        }

        return itemService.updateItem(id, request).map(Item::toJson);
    }

    /**
     * Deletes an item.
     *
     * @param request the request containing the item ID
     * @return a Future with the result
     */
    @Override
    public Future<JsonObject> deleteItem(JsonObject request) {
        logger.debug("Deleting item: {}", request);

        String id = request.getString("id");
        if (id == null) {
            return Future.failedFuture("ID is required for delete");
        }

        return itemService.deleteItem(id);
    }

    /**
     * Lists all items.
     *
     * @return a Future with the list of items
     */
    @Override
    public Future<JsonObject> listItems() {
        logger.debug("Listing all items");
        return itemService.listItems();
    }
}
