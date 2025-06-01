package dev.mars.vertx.service.one.service;

import dev.mars.vertx.service.one.model.Item;
import dev.mars.vertx.service.one.repository.ItemRepositoryInterface;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ItemServiceTest {

    private MockItemRepository mockRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        mockRepository = new MockItemRepository();
        itemService = new ItemService(mockRepository);
    }

    @Test
    void testInitialize(VertxTestContext testContext) {
        itemService.initialize()
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertTrue(mockRepository.isInitialized());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetItem(VertxTestContext testContext) {
        // Add a test item to the repository
        Item testItem = new Item("test-id", "Test Item", "Test Description", 1000L, 2000L);
        mockRepository.addItem(testItem);

        // Call the service method
        itemService.getItem("test-id")
            .onComplete(testContext.succeeding(item -> {
                testContext.verify(() -> {
                    assertEquals("test-id", item.getId());
                    assertEquals("Test Item", item.getName());
                    assertEquals("Test Description", item.getDescription());
                    assertEquals(1000L, item.getCreatedAt());
                    assertEquals(2000L, item.getUpdatedAt());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetItemNotFound(VertxTestContext testContext) {
        // Call the service method with a non-existent ID
        itemService.getItem("non-existent-id")
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testCreateItem(VertxTestContext testContext) {
        // Create test data
        JsonObject itemData = new JsonObject()
            .put("name", "New Item")
            .put("description", "New Description");

        // Call the service method
        itemService.createItem(itemData)
            .onComplete(testContext.succeeding(item -> {
                testContext.verify(() -> {
                    assertNotNull(item.getId());
                    assertEquals("New Item", item.getName());
                    assertEquals("New Description", item.getDescription());
                    assertTrue(item.getCreatedAt() > 0);
                    assertNull(item.getUpdatedAt());
                    
                    // Verify the item was added to the repository
                    Item savedItem = mockRepository.getItems().get(item.getId());
                    assertNotNull(savedItem);
                    assertEquals(item.getId(), savedItem.getId());
                    assertEquals("New Item", savedItem.getName());
                    
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testCreateItemWithDefaults(VertxTestContext testContext) {
        // Create test data with missing fields
        JsonObject itemData = new JsonObject();

        // Call the service method
        itemService.createItem(itemData)
            .onComplete(testContext.succeeding(item -> {
                testContext.verify(() -> {
                    assertNotNull(item.getId());
                    assertEquals("Unnamed", item.getName());
                    assertEquals("", item.getDescription());
                    assertTrue(item.getCreatedAt() > 0);
                    assertNull(item.getUpdatedAt());
                    
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUpdateItem(VertxTestContext testContext) {
        // Add a test item to the repository
        Item existingItem = new Item("test-id", "Original Name", "Original Description", 1000L, null);
        mockRepository.addItem(existingItem);

        // Create update data
        JsonObject updateData = new JsonObject()
            .put("name", "Updated Name")
            .put("description", "Updated Description");

        // Call the service method
        itemService.updateItem("test-id", updateData)
            .onComplete(testContext.succeeding(item -> {
                testContext.verify(() -> {
                    assertEquals("test-id", item.getId());
                    assertEquals("Updated Name", item.getName());
                    assertEquals("Updated Description", item.getDescription());
                    assertEquals(1000L, item.getCreatedAt());
                    assertNotNull(item.getUpdatedAt());
                    
                    // Verify the item was updated in the repository
                    Item updatedItem = mockRepository.getItems().get("test-id");
                    assertEquals("Updated Name", updatedItem.getName());
                    assertEquals("Updated Description", updatedItem.getDescription());
                    
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUpdateItemPartial(VertxTestContext testContext) {
        // Add a test item to the repository
        Item existingItem = new Item("test-id", "Original Name", "Original Description", 1000L, null);
        mockRepository.addItem(existingItem);

        // Create update data with only name
        JsonObject updateData = new JsonObject()
            .put("name", "Updated Name");

        // Call the service method
        itemService.updateItem("test-id", updateData)
            .onComplete(testContext.succeeding(item -> {
                testContext.verify(() -> {
                    assertEquals("test-id", item.getId());
                    assertEquals("Updated Name", item.getName());
                    assertEquals("Original Description", item.getDescription());
                    assertEquals(1000L, item.getCreatedAt());
                    assertNotNull(item.getUpdatedAt());
                    
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUpdateItemNotFound(VertxTestContext testContext) {
        // Create update data
        JsonObject updateData = new JsonObject()
            .put("name", "Updated Name");

        // Call the service method with a non-existent ID
        itemService.updateItem("non-existent-id", updateData)
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testDeleteItem(VertxTestContext testContext) {
        // Add a test item to the repository
        Item testItem = new Item("test-id", "Test Item", "Test Description", 1000L, null);
        mockRepository.addItem(testItem);

        // Call the service method
        itemService.deleteItem("test-id")
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertTrue(result.getBoolean("success"));
                    assertEquals("Item deleted successfully", result.getString("message"));
                    assertEquals("test-id", result.getString("id"));
                    
                    // Verify the item was removed from the repository
                    assertFalse(mockRepository.getItems().containsKey("test-id"));
                    
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testDeleteItemNotFound(VertxTestContext testContext) {
        // Call the service method with a non-existent ID
        itemService.deleteItem("non-existent-id")
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testListItems(VertxTestContext testContext) {
        // Add test items to the repository
        mockRepository.addItem(new Item("id1", "Item 1", "Description 1", 1000L, null));
        mockRepository.addItem(new Item("id2", "Item 2", "Description 2", 2000L, 3000L));

        // Call the service method
        itemService.listItems()
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertTrue(result.containsKey("items"));
                    assertTrue(result.containsKey("count"));
                    assertEquals(2, result.getInteger("count"));
                    
                    testContext.completeNow();
                });
            }));
    }

    /**
     * Mock implementation of ItemRepositoryInterface for testing.
     */
    private static class MockItemRepository implements ItemRepositoryInterface {
        private final Map<String, Item> items = new HashMap<>();
        private boolean initialized = false;

        public Map<String, Item> getItems() {
            return items;
        }

        public boolean isInitialized() {
            return initialized;
        }

        public void addItem(Item item) {
            items.put(item.getId(), item);
        }

        @Override
        public Future<Item> findById(String id) {
            Item item = items.get(id);
            if (item != null) {
                return Future.succeededFuture(item);
            } else {
                return Future.failedFuture("Item not found with ID: " + id);
            }
        }

        @Override
        public Future<List<Item>> findAll() {
            return Future.succeededFuture(new ArrayList<>(items.values()));
        }

        @Override
        public Future<Item> save(Item item) {
            if (item.getId() == null || item.getId().isEmpty()) {
                // Create new item
                item.setId(UUID.randomUUID().toString());
                item.setCreatedAt(System.currentTimeMillis());
                items.put(item.getId(), item);
                return Future.succeededFuture(item);
            } else {
                // Update existing item
                if (!items.containsKey(item.getId())) {
                    return Future.failedFuture("Item not found with ID: " + item.getId());
                }
                
                Item existingItem = items.get(item.getId());
                item.setCreatedAt(existingItem.getCreatedAt());
                item.setUpdatedAt(System.currentTimeMillis());
                items.put(item.getId(), item);
                return Future.succeededFuture(item);
            }
        }

        @Override
        public Future<Boolean> deleteById(String id) {
            if (items.containsKey(id)) {
                items.remove(id);
                return Future.succeededFuture(true);
            } else {
                return Future.succeededFuture(false);
            }
        }

        @Override
        public Future<Void> initialize() {
            initialized = true;
            return Future.succeededFuture();
        }

        @Override
        public Future<Integer> count() {
            return Future.succeededFuture(items.size());
        }
    }
}