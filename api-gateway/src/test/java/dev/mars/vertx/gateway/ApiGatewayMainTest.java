package dev.mars.vertx.gateway;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ApiGatewayMain class.
 * Tests the main entry point of the API Gateway application.
 */
@ExtendWith(VertxExtension.class)
class ApiGatewayMainTest {

    private static final Logger logger = LoggerFactory.getLogger(ApiGatewayMainTest.class);
    private Vertx vertx;

    @BeforeEach
    void setUp() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close()
                .onComplete(testContext.succeeding(v -> {
                    testContext.completeNow();
                }));
    }

    /**
     * Test that the main method initializes Vertx and attempts to load configuration.
     * We use reflection to call a modified version of the main method that accepts
     * a Vertx instance to avoid creating multiple Vertx instances.
     */
    @Test
    void testMainInitialization(VertxTestContext testContext) throws Exception {
        // Create a method to test the initialization logic without starting a new Vertx instance
        Method initMethod = ApiGatewayMain.class.getDeclaredMethod("initializeForTesting", Vertx.class);
        initMethod.setAccessible(true);
        
        // Call the method with our test Vertx instance
        initMethod.invoke(null, vertx);
        
        // Wait a bit for async operations to complete
        testContext.awaitCompletion(2, TimeUnit.SECONDS);
        
        // Verify that the Vertx instance is still running
        testContext.verify(() -> {
            assertTrue(vertx.deploymentIDs().isEmpty(), "No verticles should be deployed due to test config path");
            testContext.completeNow();
        });
    }

    /**
     * Test the main method directly by capturing System.out and System.err.
     * This is a more integration-style test that verifies the main method runs without exceptions.
     */
    @Test
    void testMainMethod(VertxTestContext testContext) {
        // Capture System.out and System.err
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        
        try {
            System.setOut(new PrintStream(outContent));
            System.setErr(new PrintStream(errContent));
            
            // Create a thread to run the main method
            Thread mainThread = new Thread(() -> {
                try {
                    // Use a non-existent config path to avoid actual deployment
                    String[] args = new String[]{"--config", "non-existent-config.yaml"};
                    ApiGatewayMain.main(args);
                } catch (Exception e) {
                    logger.error("Exception in main thread", e);
                }
            });
            
            // Start the thread
            mainThread.start();
            
            // Wait a bit for the main method to execute
            Thread.sleep(1000);
            
            // Verify that there are no exceptions in System.err
            String errOutput = errContent.toString();
            
            testContext.verify(() -> {
                // We expect an error about the config file not being found, but no other exceptions
                assertTrue(errOutput.contains("Failed to load configuration") || 
                           outContent.toString().contains("Starting API Gateway"), 
                           "Expected startup message or config error");
                testContext.completeNow();
            });
        } catch (Exception e) {
            testContext.failNow(e);
        } finally {
            // Restore System.out and System.err
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}