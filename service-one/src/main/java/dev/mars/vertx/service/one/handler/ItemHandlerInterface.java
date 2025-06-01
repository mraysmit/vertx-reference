package dev.mars.vertx.service.one.handler;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Interface for item-related request handlers.
 * Defines CRUD operations for handling item requests.
 */
public interface ItemHandlerInterface {
    
    /**
     * Handles a request from the event bus.
     *
     * @param request the request
     * @return a Future with the response
     */
    Future<Object> handleRequest(JsonObject request);
    
    /**
     * Gets an item by ID.
     *
     * @param id the item ID
     * @return a Future with the item as a JsonObject
     */
    Future<JsonObject> getItem(String id);
    
    /**
     * Creates a new item.
     *
     * @param request the request containing the item data
     * @return a Future with the created item as a JsonObject
     */
    Future<JsonObject> createItem(JsonObject request);
    
    /**
     * Updates an existing item.
     *
     * @param request the request containing the item data
     * @return a Future with the updated item as a JsonObject
     */
    Future<JsonObject> updateItem(JsonObject request);
    
    /**
     * Deletes an item.
     *
     * @param request the request containing the item ID
     * @return a Future with the result
     */
    Future<JsonObject> deleteItem(JsonObject request);
    
    /**
     * Lists all items.
     *
     * @return a Future with the list of items
     */
    Future<JsonObject> listItems();
}