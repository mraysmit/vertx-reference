package dev.mars.vertx.service.one.model;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    @Test
    void testDefaultConstructor() {
        Item item = new Item();
        assertNull(item.getId());
        assertNull(item.getName());
        assertNull(item.getDescription());
        assertEquals(0, item.getCreatedAt());
        assertNull(item.getUpdatedAt());
    }

    @Test
    void testParameterizedConstructor() {
        String id = "test-id";
        String name = "Test Item";
        String description = "Test Description";
        long createdAt = System.currentTimeMillis();
        Long updatedAt = createdAt + 1000;

        Item item = new Item(id, name, description, createdAt, updatedAt);
        
        assertEquals(id, item.getId());
        assertEquals(name, item.getName());
        assertEquals(description, item.getDescription());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals(updatedAt, item.getUpdatedAt());
    }

    @Test
    void testFromJson() {
        String id = "test-id";
        String name = "Test Item";
        String description = "Test Description";
        long createdAt = System.currentTimeMillis();
        Long updatedAt = createdAt + 1000;

        JsonObject json = new JsonObject()
                .put("id", id)
                .put("name", name)
                .put("description", description)
                .put("createdAt", createdAt)
                .put("updatedAt", updatedAt);

        Item item = Item.fromJson(json);
        
        assertEquals(id, item.getId());
        assertEquals(name, item.getName());
        assertEquals(description, item.getDescription());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals(updatedAt, item.getUpdatedAt());
    }

    @Test
    void testFromJsonWithDefaults() {
        String id = "test-id";
        
        JsonObject json = new JsonObject()
                .put("id", id);

        Item item = Item.fromJson(json);
        
        assertEquals(id, item.getId());
        assertEquals("", item.getName());
        assertEquals("", item.getDescription());
        assertTrue(item.getCreatedAt() > 0);
        assertNull(item.getUpdatedAt());
    }

    @Test
    void testFromJsonNull() {
        assertNull(Item.fromJson(null));
    }

    @Test
    void testToJson() {
        String id = "test-id";
        String name = "Test Item";
        String description = "Test Description";
        long createdAt = System.currentTimeMillis();
        Long updatedAt = createdAt + 1000;

        Item item = new Item(id, name, description, createdAt, updatedAt);
        JsonObject json = item.toJson();
        
        assertEquals(id, json.getString("id"));
        assertEquals(name, json.getString("name"));
        assertEquals(description, json.getString("description"));
        assertEquals(createdAt, json.getLong("createdAt"));
        assertEquals(updatedAt, json.getLong("updatedAt"));
    }

    @Test
    void testToJsonWithoutUpdatedAt() {
        String id = "test-id";
        String name = "Test Item";
        String description = "Test Description";
        long createdAt = System.currentTimeMillis();

        Item item = new Item(id, name, description, createdAt, null);
        JsonObject json = item.toJson();
        
        assertEquals(id, json.getString("id"));
        assertEquals(name, json.getString("name"));
        assertEquals(description, json.getString("description"));
        assertEquals(createdAt, json.getLong("createdAt"));
        assertFalse(json.containsKey("updatedAt"));
    }

    @Test
    void testSettersAndGetters() {
        Item item = new Item();
        
        String id = "test-id";
        String name = "Test Item";
        String description = "Test Description";
        long createdAt = System.currentTimeMillis();
        Long updatedAt = createdAt + 1000;
        
        item.setId(id);
        item.setName(name);
        item.setDescription(description);
        item.setCreatedAt(createdAt);
        item.setUpdatedAt(updatedAt);
        
        assertEquals(id, item.getId());
        assertEquals(name, item.getName());
        assertEquals(description, item.getDescription());
        assertEquals(createdAt, item.getCreatedAt());
        assertEquals(updatedAt, item.getUpdatedAt());
    }

    @Test
    void testEquals() {
        Item item1 = new Item("id1", "name1", "desc1", 1000L, 2000L);
        Item item2 = new Item("id1", "name2", "desc2", 3000L, 4000L);
        Item item3 = new Item("id2", "name1", "desc1", 1000L, 2000L);
        
        assertEquals(item1, item1);
        assertEquals(item1, item2); // Same ID, different other fields
        assertNotEquals(item1, item3); // Different ID
        assertNotEquals(item1, null);
        assertNotEquals(item1, "not an item");
    }

    @Test
    void testHashCode() {
        Item item1 = new Item("id1", "name1", "desc1", 1000L, 2000L);
        Item item2 = new Item("id1", "name2", "desc2", 3000L, 4000L);
        Item item3 = new Item("id2", "name1", "desc1", 1000L, 2000L);
        
        assertEquals(item1.hashCode(), item2.hashCode()); // Same ID
        assertNotEquals(item1.hashCode(), item3.hashCode()); // Different ID
    }

    @Test
    void testToString() {
        Item item = new Item("id1", "name1", "desc1", 1000L, 2000L);
        String toString = item.toString();
        
        assertTrue(toString.contains("id1"));
        assertTrue(toString.contains("name1"));
        assertTrue(toString.contains("desc1"));
        assertTrue(toString.contains("1000"));
        assertTrue(toString.contains("2000"));
    }
}