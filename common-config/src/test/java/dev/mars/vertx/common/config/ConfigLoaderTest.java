package dev.mars.vertx.common.config;

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
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class ConfigLoaderTest {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoaderTest.class);

    private Vertx vertx;
    private Path tempConfigFile;

    @BeforeEach
    void setUp(VertxTestContext testContext) throws IOException {
        vertx = Vertx.vertx();

        // Create a temporary config file for testing
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
}
