package dev.mars.vertx.service.one.repository;

import dev.mars.vertx.service.one.model.Item;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class InMemoryItemRepositoryTest {

    private Vertx vertx;
    private ItemRepositoryInterface repository;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        repository = new InMemoryItemRepository();

        // Initialize the repository with sample data
        repository.initialize()
            .onComplete(testContext.succeedingThenComplete());
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void testInitialize(VertxTestContext testContext) {
        repository.count()
            .onComplete(testContext.succeeding(count -> {
                testContext.verify(() -> {
                    assertEquals(5, count);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testFindById(VertxTestContext testContext) {
        repository.findById("item-1")
            .onComplete(testContext.succeeding(item -> {
                testContext.verify(() -> {
                    assertNotNull(item);
                    assertEquals("item-1", item.getId());
                    assertEquals("Sample Item 1", item.getName());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testFindByIdNotFound(VertxTestContext testContext) {
        repository.findById("non-existent-id")
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testFindAll(VertxTestContext testContext) {
        repository.findAll()
            .onComplete(testContext.succeeding(items -> {
                testContext.verify(() -> {
                    assertNotNull(items);
                    assertEquals(5, items.size());

                    // Verify that all expected items are present
                    boolean hasItem1 = false;
                    boolean hasItem5 = false;

                    for (Item item : items) {
                        if ("item-1".equals(item.getId())) {
                            hasItem1 = true;
                        } else if ("item-5".equals(item.getId())) {
                            hasItem5 = true;
                        }
                    }

                    assertTrue(hasItem1, "Item 1 should be present");
                    assertTrue(hasItem5, "Item 5 should be present");

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testSaveNewItem(VertxTestContext testContext) {
        Item newItem = new Item();
        newItem.setName("New Test Item");
        newItem.setDescription("This is a new test item");

        repository.save(newItem)
            .compose(savedItem -> {
                // Verify the saved item has an ID and timestamps
                assertNotNull(savedItem.getId());
                assertTrue(savedItem.getCreatedAt() > 0);
                assertNull(savedItem.getUpdatedAt());

                // Now try to find the item by ID
                return repository.findById(savedItem.getId());
            })
            .onComplete(testContext.succeeding(foundItem -> {
                testContext.verify(() -> {
                    assertNotNull(foundItem);
                    assertEquals("New Test Item", foundItem.getName());
                    assertEquals("This is a new test item", foundItem.getDescription());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUpdateItem(VertxTestContext testContext) {
        repository.findById("item-2")
            .compose(item -> {
                // Update the item
                item.setName("Updated Item 2");
                item.setDescription("This is an updated description");
                return repository.save(item);
            })
            .compose(updatedItem -> {
                // Verify the updated item
                assertEquals("Updated Item 2", updatedItem.getName());
                assertEquals("This is an updated description", updatedItem.getDescription());
                assertNotNull(updatedItem.getUpdatedAt());

                // Now try to find the item by ID
                return repository.findById("item-2");
            })
            .onComplete(testContext.succeeding(foundItem -> {
                testContext.verify(() -> {
                    assertNotNull(foundItem);
                    assertEquals("Updated Item 2", foundItem.getName());
                    assertEquals("This is an updated description", foundItem.getDescription());
                    assertNotNull(foundItem.getUpdatedAt());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testUpdateNonExistentItem(VertxTestContext testContext) {
        Item nonExistentItem = new Item();
        nonExistentItem.setId("non-existent-id");
        nonExistentItem.setName("Non-existent Item");

        repository.save(nonExistentItem)
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testDeleteById(VertxTestContext testContext) {
        repository.deleteById("item-3")
            .compose(deleted -> {
                // Verify the item was deleted
                assertTrue(deleted);

                // Now try to find the item by ID
                return repository.findById("item-3")
                    .compose(
                        item -> Future.failedFuture("Item should not be found"),
                        err -> Future.succeededFuture(true)
                    );
            })
            .compose(notFound -> repository.count())
            .onComplete(testContext.succeeding(count -> {
                testContext.verify(() -> {
                    assertEquals(4, count);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testDeleteByIdNotFound(VertxTestContext testContext) {
        repository.deleteById("non-existent-id")
            .onComplete(testContext.succeeding(deleted -> {
                testContext.verify(() -> {
                    assertFalse(deleted);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testCount(VertxTestContext testContext) {
        repository.count()
            .onComplete(testContext.succeeding(count -> {
                testContext.verify(() -> {
                    assertEquals(5, count);
                    testContext.completeNow();
                });
            }));
    }
}
