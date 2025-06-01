package dev.mars.vertx.service.one.service;

import dev.mars.vertx.service.one.model.Item;
import dev.mars.vertx.service.one.repository.ItemRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service class for Item operations.
 * Contains business logic for handling items.
 */
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);
    
    private final ItemRepository itemRepository;
    
    /**
     * Constructor.
     *
     * @param itemRepository the item repository
     */
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }
    
    /**
     * Initializes the service with sample data.
     *
     * @return a Future that completes when initialization is done
     */
    public Future<Void> initialize() {
        logger.info("Initializing item service");
        return itemRepository.initialize();
    }
    
    /**
     * Gets an item by ID.
     *
     * @param id the item ID
     * @return a Future with the item
     */
    public Future<Item> getItem(String id) {
        logger.info("Getting item with ID: {}", id);
        return itemRepository.findById(id);
    }
    
    /**
     * Creates a new item.
     *
     * @param itemData the item data
     * @return a Future with the created item
     */
    public Future<Item> createItem(JsonObject itemData) {
        logger.info("Creating new item: {}", itemData);
        
        Item item = new Item();
        item.setName(itemData.getString("name", "Unnamed"));
        item.setDescription(itemData.getString("description", ""));
        
        return itemRepository.save(item);
    }
    
    /**
     * Updates an existing item.
     *
     * @param id the item ID
     * @param itemData the updated item data
     * @return a Future with the updated item
     */
    public Future<Item> updateItem(String id, JsonObject itemData) {
        logger.info("Updating item with ID: {}", id);
        
        return itemRepository.findById(id)
            .compose(existingItem -> {
                // Update fields if present in the request
                if (itemData.containsKey("name")) {
                    existingItem.setName(itemData.getString("name"));
                }
                
                if (itemData.containsKey("description")) {
                    existingItem.setDescription(itemData.getString("description"));
                }
                
                return itemRepository.save(existingItem);
            });
    }
    
    /**
     * Deletes an item by ID.
     *
     * @param id the item ID
     * @return a Future with a success message
     */
    public Future<JsonObject> deleteItem(String id) {
        logger.info("Deleting item with ID: {}", id);
        
        return itemRepository.deleteById(id)
            .compose(deleted -> {
                if (deleted) {
                    return Future.succeededFuture(new JsonObject()
                        .put("success", true)
                        .put("message", "Item deleted successfully")
                        .put("id", id));
                } else {
                    return Future.failedFuture("Item not found with ID: " + id);
                }
            });
    }
    
    /**
     * Lists all items.
     *
     * @return a Future with a list of all items
     */
    public Future<JsonObject> listItems() {
        logger.info("Listing all items");
        
        return itemRepository.findAll()
            .compose(items -> {
                JsonObject result = new JsonObject()
                    .put("items", items.stream().map(Item::toJson).toArray())
                    .put("count", items.size());
                
                return Future.succeededFuture(result);
            });
    }
}