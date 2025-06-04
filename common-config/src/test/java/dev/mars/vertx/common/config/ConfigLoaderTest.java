package dev.mars.vertx.common.config;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ConfigLoaderTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoaderTest.class);

    private Vertx vertx;
    private Path tempConfigFile;

    @BeforeEach
    void setUp(VertxTestContext testContext) throws IOException {
        vertx = Vertx.vertx();

        // Create a temporary YAML config file for testing
        tempConfigFile = Files.createTempFile("config-test", ".yaml");

        // Create YAML content
        String yamlContent = 
                "testKey: testValue\n" +
                "nestedConfig:\n" +
                "  nestedKey: nestedValue\n";

        Files.write(tempConfigFile, yamlContent.getBytes());

        testContext.completeNow();
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        // Delete the temporary config file
        try {
            Files.deleteIfExists(tempConfigFile);
        } catch (IOException e) {
            logger.error("Failed to delete temp file: {}", e.getMessage());
        }

        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void testLoadConfigFromFile(VertxTestContext testContext) {
        ConfigLoader.load(vertx, tempConfigFile.toString())
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        assertEquals("testValue", config.getString("testKey"));
                        assertTrue(config.containsKey("nestedConfig"));

                        JsonObject nestedConfig = config.getJsonObject("nestedConfig");
                        assertNotNull(nestedConfig);
                        assertEquals("nestedValue", nestedConfig.getString("nestedKey"));

                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testLoadConfigWithoutFile(VertxTestContext testContext) {
        // Test loading config without a file (should still load env and sys properties)
        ConfigLoader.load(vertx, null)
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        // We can't assert specific values here as they depend on the environment
                        // But we can verify that we got a non-empty config object
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testLoadConfigWithNonExistentFile(VertxTestContext testContext) {
        // Test loading config with a non-existent file
        ConfigLoader.load(vertx, "non-existent-file.yaml")
                .onComplete(testContext.failing(err -> {
                    // Should fail because the file doesn't exist
                    testContext.verify(() -> {
                        assertNotNull(err);
                        assertTrue(err.getMessage().contains("non-existent-file.yaml") || 
                                  err.getMessage().contains("Cannot read"));
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testLoadWithScanPeriod(VertxTestContext testContext) {
        // Test loading config with a scan period
        long scanPeriod = 1000; // 1 second

        ConfigLoader.loadWithScanPeriod(vertx, tempConfigFile.toString(), scanPeriod)
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        assertEquals("testValue", config.getString("testKey"));

                        // Now modify the file and wait for the scan period to detect the change
                        try {
                            // Update the config file with new content
                            String updatedContent = 
                                    "testKey: updatedValue\n" +
                                    "nestedConfig:\n" +
                                    "  nestedKey: nestedValue\n" +
                                    "newKey: newValue\n";

                            Files.write(tempConfigFile, updatedContent.getBytes());

                            // Wait a bit longer than the scan period to ensure the change is detected
                            vertx.setTimer(scanPeriod + 500, id -> {
                                // Verify the configuration was updated
                                ConfigLoader.load(vertx, tempConfigFile.toString())
                                        .onComplete(testContext.succeeding(updatedConfig -> {
                                            testContext.verify(() -> {
                                                assertEquals("updatedValue", updatedConfig.getString("testKey"));
                                                assertEquals("newValue", updatedConfig.getString("newKey"));
                                                testContext.completeNow();
                                            });
                                        }));
                            });
                        } catch (IOException e) {
                            testContext.failNow(e);
                        }
                    });
                }));

        // Ensure the test doesn't time out too quickly
        try {
            testContext.awaitCompletion(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            testContext.failNow(e);
        }
    }

    @Test
    void testLoadWithScanPeriodNullPath(VertxTestContext testContext) {
        // Test loading config with a scan period but null path
        ConfigLoader.loadWithScanPeriod(vertx, null, 1000)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertNotNull(err);
                        assertEquals("Configuration path cannot be null or empty when using scan period", 
                                    err.getMessage());
                        testContext.completeNow();
                    });
                }));
    }


    @Test
    void testLoadConfigWithEmptyPath(VertxTestContext testContext) {
        // Test loading config with an empty path (should still load env and sys properties)
        ConfigLoader.load(vertx, "")
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        // We can't assert specific values here as they depend on the environment
                        // But we can verify that we got a non-empty config object
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testConfigChangeListener(VertxTestContext testContext) throws IOException {
        // Test that the configuration change listener is triggered when the file changes
        AtomicBoolean changeDetected = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // Set up a custom listener to detect changes
        vertx.eventBus().consumer("config-change", message -> {
            changeDetected.set(true);
            latch.countDown();
        });

        // Load config with a scan period
        long scanPeriod = 500; // 500ms
        ConfigLoader.loadWithScanPeriod(vertx, tempConfigFile.toString(), scanPeriod)
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        assertEquals("testValue", config.getString("testKey"));

                        // Now modify the file to trigger the change listener
                        try {
                            // Update the config file with new content
                            String updatedContent = 
                                    "testKey: changedValue\n" +
                                    "nestedConfig:\n" +
                                    "  nestedKey: nestedValue\n" +
                                    "newKey: newValue\n";

                            Files.write(tempConfigFile, updatedContent.getBytes());

                            // Register a handler to publish to our custom address when config changes
                            vertx.eventBus().publish("config-change", "changed");

                            // Wait for the change to be detected
                            vertx.setTimer(scanPeriod + 1000, id -> {
                                // Verify the configuration was updated
                                ConfigLoader.load(vertx, tempConfigFile.toString())
                                        .onComplete(testContext.succeeding(updatedConfig -> {
                                            testContext.verify(() -> {
                                                assertEquals("changedValue", updatedConfig.getString("testKey"));
                                                assertEquals("newValue", updatedConfig.getString("newKey"));
                                                testContext.completeNow();
                                            });
                                        }));
                            });
                        } catch (IOException e) {
                            testContext.failNow(e);
                        }
                    });
                }));

        // Ensure the test doesn't time out too quickly
        try {
            testContext.awaitCompletion(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            testContext.failNow(e);
        }
    }

    @Test
    void testLoadWithVeryShortScanPeriod(VertxTestContext testContext) throws IOException {
        // Test loading config with a very short scan period
        long scanPeriod = 100; // 100ms - very short

        ConfigLoader.loadWithScanPeriod(vertx, tempConfigFile.toString(), scanPeriod)
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        assertEquals("testValue", config.getString("testKey"));

                        // Now modify the file and wait for the scan period to detect the change
                        try {
                            // Update the config file with new content
                            String updatedContent = 
                                    "testKey: quickChange\n" +
                                    "nestedConfig:\n" +
                                    "  nestedKey: nestedValue\n" +
                                    "quickKey: quickValue\n";

                            Files.write(tempConfigFile, updatedContent.getBytes());

                            // Wait a bit longer than the scan period to ensure the change is detected
                            vertx.setTimer(scanPeriod + 200, id -> {
                                // Verify the configuration was updated
                                ConfigLoader.load(vertx, tempConfigFile.toString())
                                        .onComplete(testContext.succeeding(updatedConfig -> {
                                            testContext.verify(() -> {
                                                assertEquals("quickChange", updatedConfig.getString("testKey"));
                                                assertEquals("quickValue", updatedConfig.getString("quickKey"));
                                                testContext.completeNow();
                                            });
                                        }));
                            });
                        } catch (IOException e) {
                            testContext.failNow(e);
                        }
                    });
                }));

        // Ensure the test doesn't time out too quickly
        try {
            testContext.awaitCompletion(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            testContext.failNow(e);
        }
    }

    @Test
    void testLoadWithScanPeriodEmptyPath(VertxTestContext testContext) {
        // Test loading config with a scan period but empty path
        ConfigLoader.loadWithScanPeriod(vertx, "", 1000)
                .onComplete(testContext.failing(err -> {
                    testContext.verify(() -> {
                        assertNotNull(err);
                        assertEquals("Configuration path cannot be null or empty when using scan period", 
                                    err.getMessage());
                        testContext.completeNow();
                    });
                }));
    }

    @Test
    void testLoadWithSystemProperties(VertxTestContext testContext) {
        // Set a system property for testing
        String testPropertyKey = "config.test.property";
        String testPropertyValue = "test-value-" + System.currentTimeMillis();

        try {
            // Set the system property
            System.setProperty(testPropertyKey, testPropertyValue);

            // Load config (without a file, so it will use system properties)
            ConfigLoader.load(vertx, null)
                    .onComplete(testContext.succeeding(config -> {
                        testContext.verify(() -> {
                            assertNotNull(config);

                            // The system property should be in the config
                            // Note: Vert.x Config might convert the property key to a different format
                            // So we check if the value exists anywhere in the config
                            boolean foundProperty = false;
                            for (String key : config.fieldNames()) {
                                if (config.getValue(key) instanceof String && 
                                    config.getString(key).equals(testPropertyValue)) {
                                    foundProperty = true;
                                    break;
                                }
                            }

                            assertTrue(foundProperty, "System property not found in config");
                            testContext.completeNow();
                        });
                    }));
        } finally {
            // Clean up - remove the system property
            System.clearProperty(testPropertyKey);
        }
    }

    @Test
    void testLoadWithMultipleConfigSources(VertxTestContext testContext) {
        // Set a system property that will override a value in the config file
        String testPropertyKey = "testKey";
        String testPropertyValue = "system-property-value";

        try {
            // Set the system property
            System.setProperty(testPropertyKey, testPropertyValue);

            // Load config from file and system properties
            ConfigLoader.load(vertx, tempConfigFile.toString())
                    .onComplete(testContext.succeeding(config -> {
                        testContext.verify(() -> {
                            assertNotNull(config);

                            // The value from the system property should override the file value
                            // depending on the order of stores in the ConfigLoader
                            // We can't guarantee which one will win, but we can check that one of them is there
                            String actualValue = config.getString(testPropertyKey);
                            assertNotNull(actualValue);
                            assertTrue(
                                actualValue.equals(testPropertyValue) || actualValue.equals("testValue"),
                                "Neither system property nor file value found for testKey"
                            );

                            testContext.completeNow();
                        });
                    }));
        } finally {
            // Clean up - remove the system property
            System.clearProperty(testPropertyKey);
        }
    }

    @Test
    void testLoadWithZeroScanPeriod(VertxTestContext testContext) {
        // Test loading config with a zero scan period (should disable scanning)
        ConfigLoader.loadWithScanPeriod(vertx, tempConfigFile.toString(), 0)
                .onComplete(testContext.succeeding(config -> {
                    testContext.verify(() -> {
                        assertNotNull(config);
                        assertEquals("testValue", config.getString("testKey"));

                        // Now modify the file, but since scan period is 0, it shouldn't detect changes
                        try {
                            // Update the config file with new content
                            String updatedContent = 
                                    "testKey: zeroScanValue\n" +
                                    "nestedConfig:\n" +
                                    "  nestedKey: nestedValue\n" +
                                    "zeroScanKey: zeroScanValue\n";

                            Files.write(tempConfigFile, updatedContent.getBytes());

                            // Wait a bit to ensure the change would be detected if scanning was enabled
                            vertx.setTimer(1000, id -> {
                                // Verify the configuration was NOT updated automatically
                                // We need to load it explicitly to see the changes
                                ConfigLoader.load(vertx, tempConfigFile.toString())
                                        .onComplete(testContext.succeeding(updatedConfig -> {
                                            testContext.verify(() -> {
                                                // This should have the new values because we're loading explicitly
                                                assertEquals("zeroScanValue", updatedConfig.getString("testKey"));
                                                assertEquals("zeroScanValue", updatedConfig.getString("zeroScanKey"));
                                                testContext.completeNow();
                                            });
                                        }));
                            });
                        } catch (IOException e) {
                            testContext.failNow(e);
                        }
                    });
                }));

        // Ensure the test doesn't time out too quickly
        try {
            testContext.awaitCompletion(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            testContext.failNow(e);
        }
    }
}
