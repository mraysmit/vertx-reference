package dev.mars.vertx.bootstrap;

import dev.mars.vertx.gateway.ApiGatewayVerticle;
import dev.mars.vertx.service.one.ServiceOneVerticle;
import dev.mars.vertx.service.two.ServiceTwoVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Bootstrap class for the Vertx-Reference project.
 * Starts all services in the correct sequence and demonstrates end-to-end functionality.
 */
public class VertxReferenceBootstrap {
    private static final Logger logger = LoggerFactory.getLogger(VertxReferenceBootstrap.class);

    private static final int API_GATEWAY_PORT = 8080;
    private static final String API_GATEWAY_HOST = "localhost";

    private Vertx vertx;
    private WebClient webClient;

    public static void main(String[] args) {
        logger.info("Starting Vertx-Reference Bootstrap application");
        logger.debug("Initializing bootstrap instance");
        VertxReferenceBootstrap bootstrap = new VertxReferenceBootstrap();

        try {
            logger.debug("Calling start() method");
            bootstrap.start();
            logger.debug("Start completed successfully, calling runDemonstration() method");
            bootstrap.runDemonstration();
            logger.info("Demonstration completed successfully");
        } catch (Exception e) {
            logger.error("Error in bootstrap process: {}", e.getMessage(), e);
        } finally {
            logger.debug("Entering shutdown sequence");
            bootstrap.shutdown();
            logger.info("Vertx-Reference Bootstrap application terminated");
        }
    }

    /**
     * Starts all services in the correct sequence.
     */
    public void start() throws Exception {
        logger.info("Starting Vertx-Reference Bootstrap");
        logger.debug("Method start() called");

        // Create Vertx instance
        logger.debug("Configuring Vertx options");
        VertxOptions options = new VertxOptions();
        logger.debug("Vertx options configured: {}", options);
        logger.debug("Creating Vertx instance");
        vertx = Vertx.vertx(options);
        logger.debug("Vertx instance created successfully");

        // Create Web Client for API calls
        logger.debug("Configuring WebClient options with host={}, port={}", API_GATEWAY_HOST, API_GATEWAY_PORT);
        WebClientOptions clientOptions = new WebClientOptions()
                .setDefaultHost(API_GATEWAY_HOST)
                .setDefaultPort(API_GATEWAY_PORT);
        logger.debug("Creating WebClient instance");
        webClient = WebClient.create(vertx, clientOptions);
        logger.debug("WebClient created successfully");

        // Deploy services in sequence and wait for each to complete
        logger.info("Preparing to deploy services");
        logger.debug("Creating CountDownLatch with count=3");
        CountDownLatch latch = new CountDownLatch(3);

        // 1. Deploy Service One
        logger.info("Initiating deployment of Service One");
        deployServiceOne().onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Service One deployed successfully: {}", ar.result());
                logger.debug("Service One deployment ID: {}", ar.result());
                logger.debug("Counting down latch for Service One");
                latch.countDown();
                logger.debug("Latch count after Service One deployment: {}", latch.getCount());
            } else {
                logger.error("Failed to deploy Service One: {}", ar.cause().getMessage(), ar.cause());
                logger.debug("Counting down latch for Service One despite failure");
                latch.countDown();
                logger.debug("Latch count after Service One deployment failure: {}", latch.getCount());
            }
        });

        // 2. Deploy Service Two
        logger.info("Initiating deployment of Service Two");
        deployServiceTwo().onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Service Two deployed successfully: {}", ar.result());
                logger.debug("Service Two deployment ID: {}", ar.result());
                logger.debug("Counting down latch for Service Two");
                latch.countDown();
                logger.debug("Latch count after Service Two deployment: {}", latch.getCount());
            } else {
                logger.error("Failed to deploy Service Two: {}", ar.cause().getMessage(), ar.cause());
                logger.debug("Counting down latch for Service Two despite failure");
                latch.countDown();
                logger.debug("Latch count after Service Two deployment failure: {}", latch.getCount());
            }
        });

        // 3. Deploy API Gateway
        logger.info("Initiating deployment of API Gateway");
        deployApiGateway().onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("API Gateway deployed successfully: {}", ar.result());
                logger.debug("API Gateway deployment ID: {}", ar.result());
                logger.debug("Counting down latch for API Gateway");
                latch.countDown();
                logger.debug("Latch count after API Gateway deployment: {}", latch.getCount());
            } else {
                logger.error("Failed to deploy API Gateway: {}", ar.cause().getMessage(), ar.cause());
                logger.debug("Counting down latch for API Gateway despite failure");
                latch.countDown();
                logger.debug("Latch count after API Gateway deployment failure: {}", latch.getCount());
            }
        });

        // Wait for all services to be deployed
        logger.info("Waiting for all services to be deployed (timeout: 60 seconds)");
        if (!latch.await(60, TimeUnit.SECONDS)) {
            logger.error("Timeout occurred while waiting for services to deploy");
            throw new RuntimeException("Timeout waiting for services to deploy");
        }
        logger.debug("All services deployment completed within timeout period");

        // Wait a bit for services to initialize
        logger.debug("Waiting 2 seconds for services to initialize");
        Thread.sleep(2000);
        logger.debug("Initialization wait period completed");

        logger.info("All services deployed and initialized successfully");
    }

    /**
     * Deploys the Service One verticle.
     */
    private Future<String> deployServiceOne() {
        logger.info("Deploying Service One");
        logger.debug("Method deployServiceOne() called");

        logger.debug("Configuring deployment options for Service One");
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", "service.one")
                        .put("http.port", 8081)
                );
        logger.debug("Service One deployment options configured: {}", options.toJson());

        logger.debug("Deploying ServiceOneVerticle with class name: {}", ServiceOneVerticle.class.getName());
        return vertx.deployVerticle(ServiceOneVerticle.class.getName(), options)
                .onSuccess(id -> logger.debug("ServiceOneVerticle deployment initiated with ID: {}", id))
                .onFailure(err -> logger.debug("ServiceOneVerticle deployment initiation failed: {}", err.getMessage()));
    }

    /**
     * Deploys the Service Two verticle.
     */
    private Future<String> deployServiceTwo() {
        logger.info("Deploying Service Two");
        logger.debug("Method deployServiceTwo() called");

        logger.debug("Configuring deployment options for Service Two");
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", "service.two")
                        .put("http.port", 8082)
                );
        logger.debug("Service Two deployment options configured: {}", options.toJson());

        logger.debug("Deploying ServiceTwoVerticle with class name: {}", ServiceTwoVerticle.class.getName());
        return vertx.deployVerticle(ServiceTwoVerticle.class.getName(), options)
                .onSuccess(id -> logger.debug("ServiceTwoVerticle deployment initiated with ID: {}", id))
                .onFailure(err -> logger.debug("ServiceTwoVerticle deployment initiation failed: {}", err.getMessage()));
    }

    /**
     * Deploys the API Gateway verticle.
     */
    private Future<String> deployApiGateway() {
        logger.info("Deploying API Gateway");
        logger.debug("Method deployApiGateway() called");

        logger.debug("Configuring deployment options for API Gateway");
        JsonObject servicesConfig = new JsonObject()
                .put("service-one", new JsonObject().put("address", "service.one"))
                .put("service-two", new JsonObject().put("address", "service.two"));
        logger.debug("API Gateway services configuration: {}", servicesConfig);

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", API_GATEWAY_PORT)
                        .put("services", servicesConfig)
                );
        logger.debug("API Gateway deployment options configured: {}", options.toJson());

        logger.debug("Deploying ApiGatewayVerticle with class name: {}", ApiGatewayVerticle.class.getName());
        return vertx.deployVerticle(ApiGatewayVerticle.class.getName(), options)
                .onSuccess(id -> logger.debug("ApiGatewayVerticle deployment initiated with ID: {}", id))
                .onFailure(err -> logger.debug("ApiGatewayVerticle deployment initiation failed: {}", err.getMessage()));
    }

    /**
     * Runs a demonstration of the end-to-end functionality.
     */
    public void runDemonstration() throws Exception {
        logger.info("Running demonstration of end-to-end functionality");
        logger.debug("Method runDemonstration() called");

        // Create a latch to wait for all demonstrations to complete
        logger.debug("Creating CountDownLatch with count=6 for demonstration steps");
        CountDownLatch latch = new CountDownLatch(6);

        // 1. Check health endpoint
        logger.debug("Step 1: Checking health endpoint");
        checkHealth(latch);

        // 2. Create an item in Service One
        logger.debug("Step 2: Preparing to create an item in Service One");
        JsonObject item = new JsonObject()
                .put("name", "Test Item")
                .put("description", "This is a test item created by the bootstrap");
        logger.debug("Item to create: {}", item);

        logger.debug("Calling createServiceOneItem method");
        createServiceOneItem(item, result -> {
            if (result != null) {
                String itemId = result.getString("id");
                logger.info("Created item with ID: {}", itemId);
                logger.debug("Item created successfully with details: {}", result);

                // 3. Get the item from Service One
                logger.debug("Step 3: Getting the item from Service One with ID: {}", itemId);
                getServiceOneItem(itemId, latch);

                // 4. Update the item in Service One
                logger.debug("Step 4: Preparing to update the item in Service One");
                JsonObject updatedItem = new JsonObject()
                        .put("name", "Updated Test Item")
                        .put("description", "This item was updated by the bootstrap");
                logger.debug("Updated item data: {}", updatedItem);

                logger.debug("Calling updateServiceOneItem method for item ID: {}", itemId);
                updateServiceOneItem(itemId, updatedItem, latch);

                // 5. List all items from Service One
                logger.debug("Step 5: Listing all items from Service One");
                listServiceOneItems(latch);

                // 6. Delete the item from Service One
                logger.debug("Step 6: Deleting the item from Service One with ID: {}", itemId);
                deleteServiceOneItem(itemId, latch);
            } else {
                logger.error("Failed to create item in Service One");
                logger.debug("Item creation failed, counting down latch for remaining steps");
                logger.debug("Counting down latch for getServiceOneItem step");
                latch.countDown();
                logger.debug("Counting down latch for updateServiceOneItem step");
                latch.countDown();
                logger.debug("Counting down latch for listServiceOneItems step");
                latch.countDown();
                logger.debug("Counting down latch for deleteServiceOneItem step");
                latch.countDown();
            }
            logger.debug("Counting down latch for createServiceOneItem step");
            latch.countDown();
            logger.debug("Latch count after item creation step: {}", latch.getCount());
        });

        // Wait for all demonstrations to complete
        logger.info("Waiting for all demonstration steps to complete (timeout: 30 seconds)");
        if (!latch.await(30, TimeUnit.SECONDS)) {
            logger.error("Timeout occurred while waiting for demonstration steps to complete");
            throw new RuntimeException("Timeout waiting for demonstrations to complete");
        }
        logger.debug("All demonstration steps completed within timeout period");

        logger.info("Demonstration completed successfully");
    }

    /**
     * Checks the health endpoint.
     */
    private void checkHealth(CountDownLatch latch) {
        logger.info("Checking health endpoint");
        logger.debug("Method checkHealth() called");

        logger.debug("Sending GET request to /health endpoint");
        webClient.get("/health")
                .send()
                .onSuccess(response -> {
                    int statusCode = response.statusCode();
                    JsonObject responseBody = response.bodyAsJsonObject();
                    logger.info("Health check response: {}", responseBody);
                    logger.debug("Health check response status code: {}", statusCode);
                    logger.debug("Health check response headers: {}", response.headers());
                    logger.debug("Counting down latch for health check step");
                    latch.countDown();
                    logger.debug("Latch count after health check: {}", latch.getCount());
                })
                .onFailure(err -> {
                    logger.error("Health check failed: {}", err.getMessage(), err);
                    logger.debug("Health check error type: {}", err.getClass().getName());
                    logger.debug("Counting down latch for health check step despite failure");
                    latch.countDown();
                    logger.debug("Latch count after health check failure: {}", latch.getCount());
                });
    }

    /**
     * Creates an item in Service One.
     */
    private void createServiceOneItem(JsonObject item, java.util.function.Consumer<JsonObject> resultHandler) {
        logger.info("Creating item in Service One: {}", item);
        logger.debug("Method createServiceOneItem() called");

        logger.debug("Preparing POST request to /api/service-one endpoint");
        logger.debug("Request payload: {}", item);
        webClient.post("/api/service-one")
                .sendJsonObject(item)
                .onSuccess(response -> {
                    int statusCode = response.statusCode();
                    JsonObject result = response.bodyAsJsonObject();
                    logger.info("Create item response: {}", result);
                    logger.debug("Create item response status code: {}", statusCode);
                    logger.debug("Create item response headers: {}", response.headers());
                    logger.debug("Calling result handler with response data");
                    resultHandler.accept(result);
                })
                .onFailure(err -> {
                    logger.error("Create item failed: {}", err.getMessage(), err);
                    logger.debug("Create item error type: {}", err.getClass().getName());
                    logger.debug("Calling result handler with null due to failure");
                    resultHandler.accept(null);
                });
    }

    /**
     * Gets an item from Service One.
     */
    private void getServiceOneItem(String itemId, CountDownLatch latch) {
        logger.info("Getting item from Service One: {}", itemId);
        logger.debug("Method getServiceOneItem() called with itemId: {}", itemId);

        String endpoint = "/api/service-one/" + itemId;
        logger.debug("Preparing GET request to endpoint: {}", endpoint);
        webClient.get(endpoint)
                .send()
                .onSuccess(response -> {
                    int statusCode = response.statusCode();
                    JsonObject responseBody = response.bodyAsJsonObject();
                    logger.info("Get item response: {}", responseBody);
                    logger.debug("Get item response status code: {}", statusCode);
                    logger.debug("Get item response headers: {}", response.headers());
                    logger.debug("Counting down latch for get item step");
                    latch.countDown();
                    logger.debug("Latch count after get item: {}", latch.getCount());
                })
                .onFailure(err -> {
                    logger.error("Get item failed: {}", err.getMessage(), err);
                    logger.debug("Get item error type: {}", err.getClass().getName());
                    logger.debug("Counting down latch for get item step despite failure");
                    latch.countDown();
                    logger.debug("Latch count after get item failure: {}", latch.getCount());
                });
    }

    /**
     * Updates an item in Service One.
     */
    private void updateServiceOneItem(String itemId, JsonObject item, CountDownLatch latch) {
        logger.info("Updating item in Service One: {}", itemId);
        logger.debug("Method updateServiceOneItem() called with itemId: {} and item: {}", itemId, item);

        String endpoint = "/api/service-one/" + itemId;
        logger.debug("Preparing PUT request to endpoint: {}", endpoint);
        logger.debug("Update payload: {}", item);
        webClient.put(endpoint)
                .sendJsonObject(item)
                .onSuccess(response -> {
                    int statusCode = response.statusCode();
                    JsonObject responseBody = response.bodyAsJsonObject();
                    logger.info("Update item response: {}", responseBody);
                    logger.debug("Update item response status code: {}", statusCode);
                    logger.debug("Update item response headers: {}", response.headers());
                    logger.debug("Counting down latch for update item step");
                    latch.countDown();
                    logger.debug("Latch count after update item: {}", latch.getCount());
                })
                .onFailure(err -> {
                    logger.error("Update item failed: {}", err.getMessage(), err);
                    logger.debug("Update item error type: {}", err.getClass().getName());
                    logger.debug("Counting down latch for update item step despite failure");
                    latch.countDown();
                    logger.debug("Latch count after update item failure: {}", latch.getCount());
                });
    }

    /**
     * Lists all items from Service One.
     */
    private void listServiceOneItems(CountDownLatch latch) {
        logger.info("Listing all items from Service One");
        logger.debug("Method listServiceOneItems() called");

        String endpoint = "/api/service-one";
        logger.debug("Preparing GET request to endpoint: {}", endpoint);
        webClient.get(endpoint)
                .send()
                .onSuccess(response -> {
                    int statusCode = response.statusCode();
                    JsonObject responseBody = response.bodyAsJsonObject();
                    logger.info("List items response: {}", responseBody);
                    logger.debug("List items response status code: {}", statusCode);
                    logger.debug("List items response headers: {}", response.headers());
                    logger.debug("Counting down latch for list items step");
                    latch.countDown();
                    logger.debug("Latch count after list items: {}", latch.getCount());
                })
                .onFailure(err -> {
                    logger.error("List items failed: {}", err.getMessage(), err);
                    logger.debug("List items error type: {}", err.getClass().getName());
                    logger.debug("Counting down latch for list items step despite failure");
                    latch.countDown();
                    logger.debug("Latch count after list items failure: {}", latch.getCount());
                });
    }

    /**
     * Deletes an item from Service One.
     */
    private void deleteServiceOneItem(String itemId, CountDownLatch latch) {
        logger.info("Deleting item from Service One: {}", itemId);
        logger.debug("Method deleteServiceOneItem() called with itemId: {}", itemId);

        String endpoint = "/api/service-one/" + itemId;
        logger.debug("Preparing DELETE request to endpoint: {}", endpoint);
        webClient.delete(endpoint)
                .send()
                .onSuccess(response -> {
                    int statusCode = response.statusCode();
                    JsonObject responseBody = response.bodyAsJsonObject();
                    logger.info("Delete item response: {}", responseBody);
                    logger.debug("Delete item response status code: {}", statusCode);
                    logger.debug("Delete item response headers: {}", response.headers());
                    logger.debug("Counting down latch for delete item step");
                    latch.countDown();
                    logger.debug("Latch count after delete item: {}", latch.getCount());
                })
                .onFailure(err -> {
                    logger.error("Delete item failed: {}", err.getMessage(), err);
                    logger.debug("Delete item error type: {}", err.getClass().getName());
                    logger.debug("Counting down latch for delete item step despite failure");
                    latch.countDown();
                    logger.debug("Latch count after delete item failure: {}", latch.getCount());
                });
    }

    /**
     * Gets the Vertx instance.
     * 
     * @return the Vertx instance
     */
    public Vertx getVertx() {
        logger.debug("Method getVertx() called");
        logger.debug("Returning Vertx instance: {}", vertx);
        return vertx;
    }

    /**
     * Shuts down the Vertx instance.
     */
    public void shutdown() {
        logger.info("Shutting down Vertx-Reference Bootstrap");
        logger.debug("Method shutdown() called");

        if (vertx != null) {
            logger.debug("Vertx instance exists, preparing to close it");
            logger.debug("Creating CountDownLatch with count=1 for shutdown process");
            CountDownLatch latch = new CountDownLatch(1);

            logger.debug("Calling vertx.close() method");
            vertx.close(ar -> {
                if (ar.succeeded()) {
                    logger.info("Vertx closed successfully");
                    logger.debug("Vertx instance closed without errors");
                } else {
                    logger.error("Failed to close Vertx: {}", ar.cause().getMessage(), ar.cause());
                    logger.debug("Vertx close error type: {}", ar.cause().getClass().getName());
                }
                logger.debug("Counting down latch for Vertx close operation");
                latch.countDown();
                logger.debug("Latch count after Vertx close: {}", latch.getCount());
            });

            try {
                logger.debug("Waiting for Vertx to close (timeout: 10 seconds)");
                boolean completed = latch.await(10, TimeUnit.SECONDS);
                if (completed) {
                    logger.debug("Vertx close completed within timeout period");
                } else {
                    logger.warn("Timeout occurred while waiting for Vertx to close");
                }
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for Vertx to close: {}", e.getMessage(), e);
                logger.debug("Restoring interrupted status");
                Thread.currentThread().interrupt();
            }
        } else {
            logger.debug("Vertx instance is null, no need to close");
        }

        logger.info("Shutdown complete");
        logger.debug("Bootstrap shutdown process finished");
    }
}
