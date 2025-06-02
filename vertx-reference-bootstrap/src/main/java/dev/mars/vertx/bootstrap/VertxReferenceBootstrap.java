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
        VertxReferenceBootstrap bootstrap = new VertxReferenceBootstrap();

        try {
            bootstrap.start();
            bootstrap.runDemonstration();
        } catch (Exception e) {
            logger.error("Error in bootstrap", e);
        } finally {
            bootstrap.shutdown();
        }
    }

    /**
     * Starts all services in the correct sequence.
     */
    public void start() throws Exception {
        logger.info("Starting Vertx-Reference Bootstrap");

        // Create Vertx instance
        VertxOptions options = new VertxOptions();
        vertx = Vertx.vertx(options);

        // Create Web Client for API calls
        WebClientOptions clientOptions = new WebClientOptions()
                .setDefaultHost(API_GATEWAY_HOST)
                .setDefaultPort(API_GATEWAY_PORT);
        webClient = WebClient.create(vertx, clientOptions);

        // Deploy services in sequence and wait for each to complete
        CountDownLatch latch = new CountDownLatch(3);

        // 1. Deploy Service One
        deployServiceOne().onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Service One deployed successfully: {}", ar.result());
                latch.countDown();
            } else {
                logger.error("Failed to deploy Service One", ar.cause());
            }
        });

        // 2. Deploy Service Two
        deployServiceTwo().onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("Service Two deployed successfully: {}", ar.result());
                latch.countDown();
            } else {
                logger.error("Failed to deploy Service Two", ar.cause());
            }
        });

        // 3. Deploy API Gateway
        deployApiGateway().onComplete(ar -> {
            if (ar.succeeded()) {
                logger.info("API Gateway deployed successfully: {}", ar.result());
                latch.countDown();
            } else {
                logger.error("Failed to deploy API Gateway", ar.cause());
            }
        });

        // Wait for all services to be deployed
        if (!latch.await(60, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout waiting for services to deploy");
        }

        // Wait a bit for services to initialize
        Thread.sleep(2000);

        logger.info("All services deployed successfully");
    }

    /**
     * Deploys the Service One verticle.
     */
    private Future<String> deployServiceOne() {
        logger.info("Deploying Service One");

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", "service.one")
                        .put("http.port", 8081)
                );

        return vertx.deployVerticle(ServiceOneVerticle.class.getName(), options);
    }

    /**
     * Deploys the Service Two verticle.
     */
    private Future<String> deployServiceTwo() {
        logger.info("Deploying Service Two");

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", "service.two")
                        .put("http.port", 8082)
                );

        return vertx.deployVerticle(ServiceTwoVerticle.class.getName(), options);
    }

    /**
     * Deploys the API Gateway verticle.
     */
    private Future<String> deployApiGateway() {
        logger.info("Deploying API Gateway");

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", API_GATEWAY_PORT)
                        .put("services", new JsonObject()
                                .put("service-one", new JsonObject()
                                        .put("address", "service.one"))
                                .put("service-two", new JsonObject()
                                        .put("address", "service.two"))
                        ));

        return vertx.deployVerticle(ApiGatewayVerticle.class.getName(), options);
    }

    /**
     * Runs a demonstration of the end-to-end functionality.
     */
    public void runDemonstration() throws Exception {
        logger.info("Running demonstration");

        // Create a latch to wait for all demonstrations to complete
        CountDownLatch latch = new CountDownLatch(6);

        // 1. Check health endpoint
        checkHealth(latch);

        // 2. Create an item in Service One
        JsonObject item = new JsonObject()
                .put("name", "Test Item")
                .put("description", "This is a test item created by the bootstrap");

        createServiceOneItem(item, result -> {
            if (result != null) {
                String itemId = result.getString("id");
                logger.info("Created item with ID: {}", itemId);

                // 3. Get the item from Service One
                getServiceOneItem(itemId, latch);

                // 4. Update the item in Service One
                JsonObject updatedItem = new JsonObject()
                        .put("name", "Updated Test Item")
                        .put("description", "This item was updated by the bootstrap");

                updateServiceOneItem(itemId, updatedItem, latch);

                // 5. List all items from Service One
                listServiceOneItems(latch);

                // 6. Delete the item from Service One
                deleteServiceOneItem(itemId, latch);
            } else {
                logger.error("Failed to create item");
                latch.countDown();
                latch.countDown();
                latch.countDown();
                latch.countDown();
            }
            latch.countDown();
        });

        // Wait for all demonstrations to complete
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new RuntimeException("Timeout waiting for demonstrations to complete");
        }

        logger.info("Demonstration completed successfully");
    }

    /**
     * Checks the health endpoint.
     */
    private void checkHealth(CountDownLatch latch) {
        logger.info("Checking health endpoint");

        webClient.get("/health")
                .send()
                .onSuccess(response -> {
                    logger.info("Health check response: {}", response.bodyAsJsonObject());
                    latch.countDown();
                })
                .onFailure(err -> {
                    logger.error("Health check failed", err);
                    latch.countDown();
                });
    }

    /**
     * Creates an item in Service One.
     */
    private void createServiceOneItem(JsonObject item, java.util.function.Consumer<JsonObject> resultHandler) {
        logger.info("Creating item in Service One: {}", item);

        webClient.post("/api/service-one")
                .sendJsonObject(item)
                .onSuccess(response -> {
                    JsonObject result = response.bodyAsJsonObject();
                    logger.info("Create item response: {}", result);
                    resultHandler.accept(result);
                })
                .onFailure(err -> {
                    logger.error("Create item failed", err);
                    resultHandler.accept(null);
                });
    }

    /**
     * Gets an item from Service One.
     */
    private void getServiceOneItem(String itemId, CountDownLatch latch) {
        logger.info("Getting item from Service One: {}", itemId);

        webClient.get("/api/service-one/" + itemId)
                .send()
                .onSuccess(response -> {
                    logger.info("Get item response: {}", response.bodyAsJsonObject());
                    latch.countDown();
                })
                .onFailure(err -> {
                    logger.error("Get item failed", err);
                    latch.countDown();
                });
    }

    /**
     * Updates an item in Service One.
     */
    private void updateServiceOneItem(String itemId, JsonObject item, CountDownLatch latch) {
        logger.info("Updating item in Service One: {}", itemId);

        webClient.put("/api/service-one/" + itemId)
                .sendJsonObject(item)
                .onSuccess(response -> {
                    logger.info("Update item response: {}", response.bodyAsJsonObject());
                    latch.countDown();
                })
                .onFailure(err -> {
                    logger.error("Update item failed", err);
                    latch.countDown();
                });
    }

    /**
     * Lists all items from Service One.
     */
    private void listServiceOneItems(CountDownLatch latch) {
        logger.info("Listing all items from Service One");

        webClient.get("/api/service-one")
                .send()
                .onSuccess(response -> {
                    logger.info("List items response: {}", response.bodyAsJsonObject());
                    latch.countDown();
                })
                .onFailure(err -> {
                    logger.error("List items failed", err);
                    latch.countDown();
                });
    }

    /**
     * Deletes an item from Service One.
     */
    private void deleteServiceOneItem(String itemId, CountDownLatch latch) {
        logger.info("Deleting item from Service One: {}", itemId);

        webClient.delete("/api/service-one/" + itemId)
                .send()
                .onSuccess(response -> {
                    logger.info("Delete item response: {}", response.bodyAsJsonObject());
                    latch.countDown();
                })
                .onFailure(err -> {
                    logger.error("Delete item failed", err);
                    latch.countDown();
                });
    }

    /**
     * Gets the Vertx instance.
     * 
     * @return the Vertx instance
     */
    public Vertx getVertx() {
        return vertx;
    }

    /**
     * Shuts down the Vertx instance.
     */
    public void shutdown() {
        logger.info("Shutting down");

        if (vertx != null) {
            CountDownLatch latch = new CountDownLatch(1);

            vertx.close(ar -> {
                if (ar.succeeded()) {
                    logger.info("Vertx closed successfully");
                } else {
                    logger.error("Failed to close Vertx", ar.cause());
                }
                latch.countDown();
            });

            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Interrupted while waiting for Vertx to close", e);
                Thread.currentThread().interrupt();
            }
        }

        logger.info("Shutdown complete");
    }
}
