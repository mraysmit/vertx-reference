package dev.mars.vertx.service.two.handler;

import dev.mars.vertx.service.two.model.Weather;
import dev.mars.vertx.service.two.service.WeatherService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for weather-related requests from the event bus.
 * Implements the WeatherHandlerInterface for weather operations.
 */
public class WeatherHandler implements WeatherHandlerInterface {
    private static final Logger logger = LoggerFactory.getLogger(WeatherHandler.class);

    private final WeatherService weatherService;

    /**
     * Constructor.
     *
     * @param weatherService the weather service
     */
    public WeatherHandler(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Handles a request from the event bus.
     *
     * @param request the request
     * @return a Future with the response
     */
    @Override
    public Future<Object> handleRequest(JsonObject request) {
        logger.info("Handling request: {}", request);

        try {
            // Check if the request has an action
            if (request.containsKey("action")) {
                String action = request.getString("action");

                switch (action) {
                    case "forecast":
                        return getForecast(request).map(forecast -> (Object) forecast);
                    case "cities":
                        return listCities().map(cities -> (Object) cities);
                    case "stats":
                        return getStats().map(stats -> (Object) stats);
                    case "list":
                        return listItems().map(items -> (Object) items);
                    default:
                        return Future.failedFuture("Unknown action: " + action);
                }
            }
            // Handle standard CRUD operations
            else if (request.containsKey("method")) {
                String method = request.getString("method");
                switch (method) {
                    case "GET":
                        if (request.containsKey("id")) {
                            return getItem(request.getString("id")).map(item -> (Object) item);
                        } else {
                            return listItems().map(items -> (Object) items);
                        }
                    case "POST":
                        return createItem(request).map(item -> (Object) item);
                    case "PUT":
                        return updateItem(request).map(item -> (Object) item);
                    case "DELETE":
                        return deleteItem(request.getString("id")).map(result -> (Object) result);
                    default:
                        return Future.failedFuture("Unknown method: " + method);
                }
            }
            // If no action but has city, return weather for that city
            else if (request.containsKey("city")) {
                String city = request.getString("city");
                return getWeatherForCity(city).map(weather -> (Object) weather);
            }
            // If has id, get item by id
            else if (request.containsKey("id")) {
                return getItem(request.getString("id")).map(item -> (Object) item);
            } else {
                // Default to returning weather for a random city
                return getRandomWeather().map(weather -> (Object) weather);
            }
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return Future.failedFuture("Error processing request: " + e.getMessage());
        }
    }

    /**
     * Gets weather data for a specific city.
     *
     * @param city the city name
     * @return a Future with the weather data as a JsonObject
     */
    @Override
    public Future<JsonObject> getWeatherForCity(String city) {
        logger.debug("Getting weather for city: {}", city);
        return weatherService.getWeatherForCity(city).map(Weather::toJson);
    }

    /**
     * Gets weather data for a random city.
     *
     * @return a Future with the weather data as a JsonObject
     */
    @Override
    public Future<JsonObject> getRandomWeather() {
        logger.debug("Getting weather for a random city");
        return weatherService.getRandomWeather().map(Weather::toJson);
    }

    /**
     * Gets a forecast for a city.
     *
     * @param request the request containing the city and days
     * @return a Future with the forecast data as a JsonObject
     */
    @Override
    public Future<JsonObject> getForecast(JsonObject request) {
        logger.debug("Getting forecast: {}", request);

        String city = request.getString("city");
        if (city == null) {
            return Future.failedFuture("City is required for forecast");
        }

        int days = request.getInteger("days", 5);
        return weatherService.getForecast(city, days);
    }

    /**
     * Lists all available cities.
     *
     * @return a Future with the list of cities as a JsonObject
     */
    @Override
    public Future<JsonObject> listCities() {
        logger.debug("Listing all cities");
        return weatherService.listCities();
    }

    /**
     * Gets statistics about the service.
     *
     * @return a Future with the statistics as a JsonObject
     */
    @Override
    public Future<JsonObject> getStats() {
        logger.debug("Getting service statistics");
        return weatherService.getStats();
    }

    /**
     * Gets an item by ID.
     *
     * @param id the item ID
     * @return a Future with the item as a JsonObject
     */
    public Future<JsonObject> getItem(String id) {
        logger.debug("Getting item by ID: {}", id);
        return weatherService.getItem(id);
    }

    /**
     * Lists all items.
     *
     * @return a Future with the list of items as a JsonObject
     */
    public Future<JsonObject> listItems() {
        logger.debug("Listing all items");
        return weatherService.listItems();
    }

    /**
     * Creates a new item.
     *
     * @param request the request containing the item data
     * @return a Future with the created item as a JsonObject
     */
    public Future<JsonObject> createItem(JsonObject request) {
        logger.debug("Creating new item: {}", request);
        return weatherService.createItem(request);
    }

    /**
     * Updates an existing item.
     *
     * @param request the request containing the item data and ID
     * @return a Future with the updated item as a JsonObject
     */
    public Future<JsonObject> updateItem(JsonObject request) {
        logger.debug("Updating item: {}", request);
        return weatherService.updateItem(request);
    }

    /**
     * Deletes an item by ID.
     *
     * @param id the item ID
     * @return a Future with the deletion result as a JsonObject
     */
    public Future<JsonObject> deleteItem(String id) {
        logger.debug("Deleting item by ID: {}", id);
        return weatherService.deleteItem(id);
    }
}
