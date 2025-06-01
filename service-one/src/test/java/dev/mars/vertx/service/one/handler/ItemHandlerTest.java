package dev.mars.vertx.service.one.handler;

import dev.mars.vertx.service.one.model.Item;
import dev.mars.vertx.service.one.service.ItemService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ItemHandlerTest {

    private MockItemService mockService;
    private ItemHandler itemHandler;

    @BeforeEach
    void setUp() {
        mockService = new MockItemService();
        itemHandler = new ItemHandler(mockService);
    }

    @Test
    void testHandleRequestGetItem(VertxTestContext testContext) {
        // Create a test item
        Item testItem = new Item("test-id", "Test Item", "Test Description", 1000L, 2000L);
        mockService.addItem(testItem);

        // Create a request with an ID
        JsonObject request = new JsonObject()
                .put("id", "test-id");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.succeeding(result -> {
                    testContext.verify(() -> {
                        // Verify the result is a JsonObject
                        assertTrue(result instanceof JsonObject);
                        JsonObject jsonResult = (JsonObject) result;

                        // Verify the item properties
                        assertEquals("test-id", jsonResult.getString("id"));
                        assertEquals("Test Item", jsonResult.getString("name"));
                        assertEquals("Test Description", jsonResult.getString("description"));
                        assertEquals(1000L, jsonResult.getLong("createdAt"));
                        assertEquals(2000L, jsonResult.getLong("updatedAt"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestGetItemNotFound(VertxTestContext testContext) {
        // Create a request with a non-existent ID
        JsonObject request = new JsonObject()
                .put("id", "non-existent-id");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertTrue(err.getMessage().contains("not found"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestCreateItem(VertxTestContext testContext) {
        // Create a request to create an item
        JsonObject request = new JsonObject()
                .put("action", "create")
                .put("name", "New Item")
                .put("description", "New Description");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.succeeding(result -> {
                    testContext.verify(() -> {
                        // Verify the result is a JsonObject
                        assertTrue(result instanceof JsonObject);
                        JsonObject jsonResult = (JsonObject) result;

                        // Verify the item properties
                        assertNotNull(jsonResult.getString("id"));
                        assertEquals("New Item", jsonResult.getString("name"));
                        assertEquals("New Description", jsonResult.getString("description"));
                        assertTrue(jsonResult.getLong("createdAt") > 0);
                        assertFalse(jsonResult.containsKey("updatedAt"));

                        // Verify the item was added to the service
                        Item item = mockService.getItems().get(jsonResult.getString("id"));
                        assertNotNull(item);
                        assertEquals("New Item", item.getName());

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestUpdateItem(VertxTestContext testContext) {
        // Create a test item
        Item testItem = new Item("test-id", "Original Name", "Original Description", 1000L, null);
        mockService.addItem(testItem);

        // Create a request to update the item
        JsonObject request = new JsonObject()
                .put("action", "update")
                .put("id", "test-id")
                .put("name", "Updated Name")
                .put("description", "Updated Description");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.succeeding(result -> {
                    testContext.verify(() -> {
                        // Verify the result is a JsonObject
                        assertTrue(result instanceof JsonObject);
                        JsonObject jsonResult = (JsonObject) result;

                        // Verify the item properties
                        assertEquals("test-id", jsonResult.getString("id"));
                        assertEquals("Updated Name", jsonResult.getString("name"));
                        assertEquals("Updated Description", jsonResult.getString("description"));
                        assertEquals(1000L, jsonResult.getLong("createdAt"));
                        assertTrue(jsonResult.containsKey("updatedAt"));

                        // Verify the item was updated in the service
                        Item item = mockService.getItems().get("test-id");
                        assertEquals("Updated Name", item.getName());
                        assertEquals("Updated Description", item.getDescription());

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestUpdateItemMissingId(VertxTestContext testContext) {
        // Create a request to update an item without an ID
        JsonObject request = new JsonObject()
                .put("action", "update")
                .put("name", "Updated Name");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertTrue(err.getMessage().contains("ID is required"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestDeleteItem(VertxTestContext testContext) {
        // Create a test item
        Item testItem = new Item("test-id", "Test Item", "Test Description", 1000L, null);
        mockService.addItem(testItem);

        // Create a request to delete the item
        JsonObject request = new JsonObject()
                .put("action", "delete")
                .put("id", "test-id");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.succeeding(result -> {
                    testContext.verify(() -> {
                        // Verify the result is a JsonObject
                        assertTrue(result instanceof JsonObject);
                        JsonObject jsonResult = (JsonObject) result;

                        // Verify the result properties
                        assertTrue(jsonResult.getBoolean("success"));
                        assertEquals("Item deleted successfully", jsonResult.getString("message"));
                        assertEquals("test-id", jsonResult.getString("id"));

                        // Verify the item was removed from the service
                        assertFalse(mockService.getItems().containsKey("test-id"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestDeleteItemMissingId(VertxTestContext testContext) {
        // Create a request to delete an item without an ID
        JsonObject request = new JsonObject()
                .put("action", "delete");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertTrue(err.getMessage().contains("ID is required"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestListItems(VertxTestContext testContext) {
        // Add test items to the service
        mockService.addItem(new Item("id1", "Item 1", "Description 1", 1000L, null));
        mockService.addItem(new Item("id2", "Item 2", "Description 2", 2000L, 3000L));

        // Create a request to list items
        JsonObject request = new JsonObject()
                .put("action", "list");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.succeeding(result -> {
                    testContext.verify(() -> {
                        // Verify the result is a JsonObject
                        assertTrue(result instanceof JsonObject);
                        JsonObject jsonResult = (JsonObject) result;

                        // Verify the result properties
                        assertTrue(jsonResult.containsKey("items"));
                        assertTrue(jsonResult.containsKey("count"));
                        assertEquals(2, jsonResult.getInteger("count"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestUnknownAction(VertxTestContext testContext) {
        // Create a request with an unknown action
        JsonObject request = new JsonObject()
                .put("action", "unknown");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertTrue(err.getMessage().contains("Unknown action"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testHandleRequestInvalidRequest(VertxTestContext testContext) {
        // Create an invalid request (no id or action)
        JsonObject request = new JsonObject()
                .put("someField", "someValue");

        // Call the handler
        itemHandler.handleRequest(request)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertTrue(err.getMessage().contains("Invalid request"));
                        testContext.completeNow();
                    });
                }));
    }

    /**
     * Mock implementation of ItemService for testing.
     */
    private static class MockItemService extends ItemService {
        private final java.util.Map<String, Item> items = new java.util.HashMap<>();

        public MockItemService() {
            super(null); // We're not using the repository in this mock
        }

        public java.util.Map<String, Item> getItems() {
            return items;
        }

        public void addItem(Item item) {
            items.put(item.getId(), item);
        }

        @Override
        public Future<Void> initialize() {
            return Future.succeededFuture();
        }

        @Override
        public Future<Item> getItem(String id) {
            Item item = items.get(id);
            if (item != null) {
                return Future.succeededFuture(item);
            } else {
                return Future.failedFuture("Item not found with ID: " + id);
            }
        }

        @Override
        public Future<Item> createItem(JsonObject itemData) {
            Item item = new Item();
            item.setId(UUID.randomUUID().toString());
            item.setName(itemData.getString("name", "Unnamed"));
            item.setDescription(itemData.getString("description", ""));
            item.setCreatedAt(System.currentTimeMillis());
            
            items.put(item.getId(), item);
            return Future.succeededFuture(item);
        }

        @Override
        public Future<Item> updateItem(String id, JsonObject itemData) {
            Item item = items.get(id);
            if (item == null) {
                return Future.failedFuture("Item not found with ID: " + id);
            }
            
            if (itemData.containsKey("name")) {
                item.setName(itemData.getString("name"));
            }
            
            if (itemData.containsKey("description")) {
                item.setDescription(itemData.getString("description"));
            }
            
            item.setUpdatedAt(System.currentTimeMillis());
            return Future.succeededFuture(item);
        }

        @Override
        public Future<JsonObject> deleteItem(String id) {
            if (items.containsKey(id)) {
                items.remove(id);
                return Future.succeededFuture(new JsonObject()
                    .put("success", true)
                    .put("message", "Item deleted successfully")
                    .put("id", id));
            } else {
                return Future.failedFuture("Item not found with ID: " + id);
            }
        }

        @Override
        public Future<JsonObject> listItems() {
            JsonObject result = new JsonObject()
                .put("items", items.values().stream().map(Item::toJson).toArray())
                .put("count", items.size());
            
            return Future.succeededFuture(result);
        }
    }
}