package dev.mars.vertx.service.two.handler;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

/**
 * Interface for weather-related request handlers.
 * Defines operations for handling weather requests.
 */
public interface WeatherHandlerInterface {
    
    /**
     * Handles a request from the event bus.
     *
     * @param request the request
     * @return a Future with the response
     */
    Future<Object> handleRequest(JsonObject request);
    
    /**
     * Gets weather data for a specific city.
     *
     * @param city the city name
     * @return a Future with the weather data as a JsonObject
     */
    Future<JsonObject> getWeatherForCity(String city);
    
    /**
     * Gets weather data for a random city.
     *
     * @return a Future with the weather data as a JsonObject
     */
    Future<JsonObject> getRandomWeather();
    
    /**
     * Gets a forecast for a city.
     *
     * @param request the request containing the city and days
     * @return a Future with the forecast data as a JsonObject
     */
    Future<JsonObject> getForecast(JsonObject request);
    
    /**
     * Lists all available cities.
     *
     * @return a Future with the list of cities as a JsonObject
     */
    Future<JsonObject> listCities();
    
    /**
     * Gets statistics about the service.
     *
     * @return a Future with the statistics as a JsonObject
     */
    Future<JsonObject> getStats();
}