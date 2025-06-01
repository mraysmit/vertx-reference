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
            // Check if the request has a city
            if (request.containsKey("city")) {
                String city = request.getString("city");
                return getWeatherForCity(city).map(weather -> (Object) weather);
            } else if (request.containsKey("action")) {
                String action = request.getString("action");

                switch (action) {
                    case "forecast":
                        return getForecast(request).map(forecast -> (Object) forecast);
                    case "cities":
                        return listCities().map(cities -> (Object) cities);
                    case "stats":
                        return getStats().map(stats -> (Object) stats);
                    default:
                        return Future.failedFuture("Unknown action: " + action);
                }
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
}