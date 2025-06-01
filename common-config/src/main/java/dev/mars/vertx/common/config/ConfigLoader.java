package dev.mars.vertx.common.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for loading configuration from various sources.
 * Supports environment variables, system properties, and configuration files.
 * This implementation includes enhanced logging for better diagnostics.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    /**
     * Loads configuration from environment variables, system properties, and a config file.
     * 
     * @param vertx the Vertx instance
     * @param configPath path to the configuration file (optional)
     * @return a Future with the loaded configuration
     */
    public static Future<JsonObject> load(Vertx vertx, String configPath) {
        logger.debug("Starting configuration loading process");
        ConfigRetrieverOptions options = new ConfigRetrieverOptions();
        
        // Add environment variables
        logger.debug("Adding environment variables store");
        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env");
        options.addStore(envStore);
        
        // Add system properties
        logger.debug("Adding system properties store");
        ConfigStoreOptions sysStore = new ConfigStoreOptions()
                .setType("sys");
        options.addStore(sysStore);
        
        // Add file configuration if provided
        if (configPath != null && !configPath.isEmpty()) {
            boolean isYaml = configPath.toLowerCase().endsWith(".yaml") || configPath.toLowerCase().endsWith(".yml");
            
            if (isYaml) {
                logger.info("Loading YAML configuration from: {}", configPath);
                // For YAML files, we use the 'file' type with a format parameter
                ConfigStoreOptions fileStore = new ConfigStoreOptions()
                        .setType("file")
                        .setFormat("yaml")
                        .setConfig(new JsonObject().put("path", configPath));
                options.addStore(fileStore);
                logger.debug("Added YAML file store with path: {}", configPath);
            } else {
                logger.info("Loading JSON configuration from: {}", configPath);
                // For JSON files, we use the 'file' type (default format is JSON)
                ConfigStoreOptions fileStore = new ConfigStoreOptions()
                        .setType("file")
                        .setConfig(new JsonObject().put("path", configPath));
                options.addStore(fileStore);
                logger.debug("Added JSON file store with path: {}", configPath);
            }
        } else {
            logger.info("No configuration file path provided, using only environment and system properties");
        }
        
        logger.debug("Creating config retriever with {} stores", options.getStores().size());
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        
        // Set up config change listener
        retriever.listen(change -> {
            logger.info("Configuration changed");
            logger.debug("Old configuration: {}", change.getPreviousConfiguration().encode());
            logger.debug("New configuration: {}", change.getNewConfiguration().encode());
        });
        
        logger.debug("Retrieving configuration");
        return retriever.getConfig()
                .onSuccess(config -> {
                    logger.info("Configuration loaded successfully with {} entries", config.size());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Configuration content: {}", config.encode());
                    }
                })
                .onFailure(err -> {
                    logger.error("Failed to load configuration: {}", err.getMessage(), err);
                    if (configPath != null && !configPath.isEmpty()) {
                        logger.error("Check if the file exists and is accessible: {}", configPath);
                    }
                });
    }
    
    /**
     * Loads configuration from a specific file with a custom scan period.
     * 
     * @param vertx the Vertx instance
     * @param configPath path to the configuration file
     * @param scanPeriodMs how often to check for changes (in milliseconds)
     * @return a Future with the loaded configuration
     */
    public static Future<JsonObject> loadWithScanPeriod(Vertx vertx, String configPath, long scanPeriodMs) {
        logger.info("Loading configuration with scan period of {} ms", scanPeriodMs);
        
        if (configPath == null || configPath.isEmpty()) {
            logger.error("Configuration path cannot be null or empty when using scan period");
            return Future.failedFuture("Configuration path cannot be null or empty when using scan period");
        }
        
        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .setScanPeriod(scanPeriodMs);
        
        boolean isYaml = configPath.toLowerCase().endsWith(".yaml") || configPath.toLowerCase().endsWith(".yml");
        
        if (isYaml) {
            logger.info("Loading YAML configuration from: {} with scan period", configPath);
            ConfigStoreOptions fileStore = new ConfigStoreOptions()
                    .setType("file")
                    .setFormat("yaml")
                    .setConfig(new JsonObject().put("path", configPath));
            options.addStore(fileStore);
        } else {
            logger.info("Loading JSON configuration from: {} with scan period", configPath);
            ConfigStoreOptions fileStore = new ConfigStoreOptions()
                    .setType("file")
                    .setConfig(new JsonObject().put("path", configPath));
            options.addStore(fileStore);
        }
        
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        
        // Set up config change listener
        retriever.listen(change -> {
            logger.info("Configuration changed (scan period: {} ms)", scanPeriodMs);
            logger.debug("Old configuration: {}", change.getPreviousConfiguration().encode());
            logger.debug("New configuration: {}", change.getNewConfiguration().encode());
        });
        
        return retriever.getConfig()
                .onSuccess(config -> {
                    logger.info("Configuration loaded successfully with {} entries (scan period: {} ms)", 
                            config.size(), scanPeriodMs);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Configuration content: {}", config.encode());
                    }
                })
                .onFailure(err -> {
                    logger.error("Failed to load configuration with scan period: {}", err.getMessage(), err);
                    logger.error("Check if the file exists and is accessible: {}", configPath);
                });
    }
}