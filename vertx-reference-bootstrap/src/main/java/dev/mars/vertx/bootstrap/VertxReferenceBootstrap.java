package dev.mars.vertx.bootstrap;

import dev.mars.vertx.common.config.ConfigLoader;
import java.io.File;
import java.nio.file.Paths;
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

    private Vertx vertx;
    private WebClient webClient;
    private JsonObject config;

    public static void main(String[] args) {
        VertxReferenceBootstrap bootstrap = new VertxReferenceBootstrap();

        bootstrap.start()
            .compose(v -> bootstrap.runDemonstration())
            .compose(v -> bootstrap.shutdown())
            .onFailure(err -> {
                logger.error("Error in bootstrap", err);
                bootstrap.shutdown();
            });
    }

    /**
     * Starts all services in the correct sequence.
     * 
     * @return A Future that completes when all services are started
     */
    public Future<Void> start() {
        logger.info("Starting Vertx-Reference Bootstrap");

        // Create Vertx instance
        VertxOptions options = new VertxOptions();
        vertx = Vertx.vertx(options);

        // Load configuration from config.yaml
        String configPath = Paths.get("src", "main", "resources", "config.yaml").toAbsolutePath().toString();
        return ConfigLoader.load(vertx, configPath)
            .compose(loadedConfig -> {
                config = loadedConfig;
                logger.info("Configuration loaded successfully");

                // Create Web Client for API calls
                String apiGatewayHost = config.getJsonObject("api-gateway", new JsonObject()).getString("host", "localhost");
                int apiGatewayPort = config.getJsonObject("api-gateway", new JsonObject()).getInteger("port", 8080);

                WebClientOptions clientOptions = new WebClientOptions()
                        .setDefaultHost(apiGatewayHost)
                        .setDefaultPort(apiGatewayPort);
                webClient = WebClient.create(vertx, clientOptions);

                // Deploy services
                return deployServices();
            });
    }

    /**
     * Deploys all services in sequence.
     * 
     * @return A Future that completes when all services are deployed
     */
    private Future<Void> deployServices() {
        logger.info("Deploying services");

        // Deploy all services in parallel and wait for all to complete
        Future<String> serviceOneFuture = deployServiceOne();
        Future<String> serviceTwoFuture = deployServiceTwo();
        Future<String> apiGatewayFuture = deployApiGateway();

        return Future.all(serviceOneFuture, serviceTwoFuture, apiGatewayFuture)
            .onSuccess(v -> logger.info("All services deployed successfully"))
            .onFailure(err -> logger.error("Failed to deploy services", err))
            .mapEmpty();
    }

    /**
     * Deploys the Service One verticle.
     */
    private Future<String> deployServiceOne() {
        logger.info("Deploying Service One");

        // Get service one configuration from loaded config
        JsonObject serviceOneConfig = config.getJsonObject("service-one", new JsonObject());
        String serviceAddress = serviceOneConfig.getString("address", "service.one");
        int httpPort = serviceOneConfig.getJsonObject("http", new JsonObject()).getInteger("port", 8081);

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", serviceAddress)
                        .put("http.port", httpPort)
                );

        logger.info("Deploying Service One with address: {} and HTTP port: {}", serviceAddress, httpPort);
        return vertx.deployVerticle(ServiceOneVerticle.class.getName(), options);
    }

    /**
     * Deploys the Service Two verticle.
     */
    private Future<String> deployServiceTwo() {
        logger.info("Deploying Service Two");

        // Get service two configuration from loaded config
        JsonObject serviceTwoConfig = config.getJsonObject("service-two", new JsonObject());
        String serviceAddress = serviceTwoConfig.getString("address", "service.two");
        int httpPort = serviceTwoConfig.getJsonObject("http", new JsonObject()).getInteger("port", 8082);

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("service.address", serviceAddress)
                        .put("http.port", httpPort)
                );

        logger.info("Deploying Service Two with address: {} and HTTP port: {}", serviceAddress, httpPort);
        return vertx.deployVerticle(ServiceTwoVerticle.class.getName(), options);
    }

    /**
     * Deploys the API Gateway verticle.
     */
    private Future<String> deployApiGateway() {
        logger.info("Deploying API Gateway");

        // Get API Gateway configuration from loaded config
        JsonObject apiGatewayConfig = config.getJsonObject("api-gateway", new JsonObject());
        int httpPort = apiGatewayConfig.getInteger("port", 8080);

        // Get services configuration
        JsonObject servicesConfig = apiGatewayConfig.getJsonObject("services", new JsonObject());

        DeploymentOptions options = new DeploymentOptions()
                .setConfig(new JsonObject()
                        .put("http.port", httpPort)
                        .put("services", servicesConfig)
                );

        logger.info("Deploying API Gateway with HTTP port: {}", httpPort);
        return vertx.deployVerticle(ApiGatewayVerticle.class.getName(), options);
    }

    /**
     * Runs a demonstration of the end-to-end functionality.
     * 
     * @return A Future that completes when the demonstration is finished
     */
    public Future<Void> runDemonstration() {
        logger.info("Running demonstration");

        // 1. Check health endpoint
        return checkHealth()
            .compose(v -> {
                // 2. Create an item in Service One
                JsonObject item = new JsonObject()
                        .put("name", "Test Item")
                        .put("description", "This is a test item created by the bootstrap");

                return createServiceOneItem(item);
            })
            .compose(result -> {
                if (result == null) {
                    logger.error("Failed to create item");
                    return Future.succeededFuture();
                }

                String itemId = result.getString("id");
                logger.info("Created item with ID: {}", itemId);

                // 3. Get the item from Service One
                Future<Void> getItemFuture = getServiceOneItem(itemId);

                // 4. Update the item in Service One
                JsonObject updatedItem = new JsonObject()
                        .put("name", "Updated Test Item")
                        .put("description", "This item was updated by the bootstrap");
                Future<Void> updateItemFuture = getItemFuture.compose(v -> 
                    updateServiceOneItem(itemId, updatedItem));

                // 5. List all items from Service One
                Future<Void> listItemsFuture = updateItemFuture.compose(v -> 
                    listServiceOneItems());

                // 6. Delete the item from Service One
                return listItemsFuture.compose(v -> 
                    deleteServiceOneItem(itemId));
            })
            .onSuccess(v -> logger.info("Demonstration completed successfully"))
            .onFailure(err -> logger.error("Demonstration failed", err));
    }

    /**
     * Checks the health endpoint.
     * 
     * @return A Future that completes when the health check is done
     */
    private Future<Void> checkHealth() {
        logger.info("Checking health endpoint");

        return webClient.get("/health")
                .send()
                .onSuccess(response -> {
                    logger.info("Health check response: {}", response.bodyAsJsonObject());
                })
                .onFailure(err -> logger.error("Health check failed", err))
                .mapEmpty();
    }

    /**
     * Creates an item in Service One.
     * 
     * @param item The item to create
     * @return A Future with the created item
     */
    private Future<JsonObject> createServiceOneItem(JsonObject item) {
        logger.info("Creating item in Service One: {}", item);

        return webClient.post("/api/service-one")
                .sendJsonObject(item)
                .map(response -> {
                    JsonObject result = response.bodyAsJsonObject();
                    logger.info("Create item response: {}", result);
                    return result;
                })
                .onFailure(err -> logger.error("Create item failed", err));
    }

    /**
     * Gets an item from Service One.
     * 
     * @param itemId The ID of the item to get
     * @return A Future that completes when the item is retrieved
     */
    private Future<Void> getServiceOneItem(String itemId) {
        logger.info("Getting item from Service One: {}", itemId);

        return webClient.get("/api/service-one/" + itemId)
                .send()
                .onSuccess(response -> {
                    logger.info("Get item response: {}", response.bodyAsJsonObject());
                })
                .onFailure(err -> logger.error("Get item failed", err))
                .mapEmpty();
    }

    /**
     * Updates an item in Service One.
     * 
     * @param itemId The ID of the item to update
     * @param item The updated item data
     * @return A Future that completes when the item is updated
     */
    private Future<Void> updateServiceOneItem(String itemId, JsonObject item) {
        logger.info("Updating item in Service One: {}", itemId);

        return webClient.put("/api/service-one/" + itemId)
                .sendJsonObject(item)
                .onSuccess(response -> {
                    logger.info("Update item response: {}", response.bodyAsJsonObject());
                })
                .onFailure(err -> logger.error("Update item failed", err))
                .mapEmpty();
    }

    /**
     * Lists all items from Service One.
     * 
     * @return A Future that completes when the items are listed
     */
    private Future<Void> listServiceOneItems() {
        logger.info("Listing all items from Service One");

        return webClient.get("/api/service-one")
                .send()
                .onSuccess(response -> {
                    logger.info("List items response: {}", response.bodyAsJsonObject());
                })
                .onFailure(err -> logger.error("List items failed", err))
                .mapEmpty();
    }

    /**
     * Deletes an item from Service One.
     * 
     * @param itemId The ID of the item to delete
     * @return A Future that completes when the item is deleted
     */
    private Future<Void> deleteServiceOneItem(String itemId) {
        logger.info("Deleting item from Service One: {}", itemId);

        return webClient.delete("/api/service-one/" + itemId)
                .send()
                .onSuccess(response -> {
                    logger.info("Delete item response: {}", response.bodyAsJsonObject());
                })
                .onFailure(err -> logger.error("Delete item failed", err))
                .mapEmpty();
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
     * 
     * @return A Future that completes when Vertx is closed
     */
    public Future<Void> shutdown() {
        logger.info("Shutting down");

        if (vertx != null) {
            return vertx.close()
                .onSuccess(v -> logger.info("Vertx closed successfully"))
                .onFailure(err -> logger.error("Failed to close Vertx", err))
                .onComplete(v -> logger.info("Shutdown complete"));
        } else {
            logger.info("Shutdown complete (no Vertx instance)");
            return Future.succeededFuture();
        }
    }
}
