package dev.mars.vertx.service.two.repository;

import dev.mars.vertx.service.two.model.Weather;
import io.vertx.core.Future;

import java.util.List;

/**
 * Repository interface for Weather operations.
 */
public interface WeatherRepository {
    
    /**
     * Finds weather data for a specific city.
     *
     * @param city the city name
     * @return a Future with the weather data, or a failed future if not found
     */
    Future<Weather> findByCity(String city);
    
    /**
     * Finds weather data for all cities.
     *
     * @return a Future with a list of weather data for all cities
     */
    Future<List<Weather>> findAll();
    
    /**
     * Saves weather data for a city.
     *
     * @param weather the weather data to save
     * @return a Future with the saved weather data
     */
    Future<Weather> save(Weather weather);
    
    /**
     * Gets a forecast for a city for a specified number of days.
     *
     * @param city the city name
     * @param days the number of days for the forecast
     * @return a Future with a list of weather data for the forecast
     */
    Future<List<Weather>> getForecast(String city, int days);
    
    /**
     * Gets a list of all available cities.
     *
     * @return a Future with a list of city names
     */
    Future<List<String>> getCities();
    
    /**
     * Initializes the repository with sample data.
     *
     * @return a Future that completes when initialization is done
     */
    Future<Void> initialize();
    
    /**
     * Gets the count of cities in the repository.
     *
     * @return a Future with the count
     */
    Future<Integer> count();
}