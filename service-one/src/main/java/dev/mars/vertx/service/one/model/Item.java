package dev.mars.vertx.service.one.model;

import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Model class representing an Item.
 */
public class Item {
    private String id;
    private String name;
    private String description;
    private long createdAt;
    private Long updatedAt;

    /**
     * Default constructor.
     */
    public Item() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id the item ID
     * @param name the item name
     * @param description the item description
     * @param createdAt the creation timestamp
     * @param updatedAt the update timestamp (can be null)
     */
    public Item(String id, String name, String description, long createdAt, Long updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates an Item from a JsonObject.
     *
     * @param json the JsonObject
     * @return the Item
     */
    public static Item fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }
        
        return new Item(
            json.getString("id"),
            json.getString("name", ""),
            json.getString("description", ""),
            json.getLong("createdAt", System.currentTimeMillis()),
            json.containsKey("updatedAt") ? json.getLong("updatedAt") : null
        );
    }

    /**
     * Converts this Item to a JsonObject.
     *
     * @return the JsonObject
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject()
            .put("id", id)
            .put("name", name)
            .put("description", description)
            .put("createdAt", createdAt);
        
        if (updatedAt != null) {
            json.put("updatedAt", updatedAt);
        }
        
        return json;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Item{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}