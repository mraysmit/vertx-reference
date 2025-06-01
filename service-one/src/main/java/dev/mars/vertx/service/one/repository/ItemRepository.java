package dev.mars.vertx.service.one.repository;

import dev.mars.vertx.service.one.model.Item;
import io.vertx.core.Future;

import java.util.List;

/**
 * Repository interface for Item operations.
 */
public interface ItemRepository {
    
    /**
     * Finds an item by ID.
     *
     * @param id the item ID
     * @return a Future with the item, or a failed future if not found
     */
    Future<Item> findById(String id);
    
    /**
     * Finds all items.
     *
     * @return a Future with a list of all items
     */
    Future<List<Item>> findAll();
    
    /**
     * Saves an item (creates or updates).
     *
     * @param item the item to save
     * @return a Future with the saved item
     */
    Future<Item> save(Item item);
    
    /**
     * Deletes an item by ID.
     *
     * @param id the item ID
     * @return a Future with true if deleted, false if not found
     */
    Future<Boolean> deleteById(String id);
    
    /**
     * Initializes the repository with sample data.
     *
     * @return a Future that completes when initialization is done
     */
    Future<Void> initialize();
    
    /**
     * Gets the count of items in the repository.
     *
     * @return a Future with the count
     */
    Future<Integer> count();
}