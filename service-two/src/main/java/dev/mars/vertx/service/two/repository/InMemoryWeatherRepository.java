package dev.mars.vertx.service.two.repository;

import dev.mars.vertx.service.two.model.Weather;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * In-memory implementation of the WeatherRepository interface.
 */
public class InMemoryWeatherRepository implements WeatherRepository {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryWeatherRepository.class);

    private final Map<String, Weather> weatherData = new HashMap<>();
    private final List<String> cities = new ArrayList<>();
    private final Random random = new Random();

    /**
     * Constructor.
     */
    public InMemoryWeatherRepository() {
        // No initialization needed
    }

    @Override
    public Future<Weather> findByCity(String city) {
        logger.debug("Finding weather data for city: {}", city);

        Promise<Weather> promise = Promise.promise();
        if (!cities.contains(city)) {
            logger.debug("City not found: {}", city);
            promise.fail("City not found: " + city);
            return promise.future();
        }

        Weather weather = weatherData.get(city);
        if (weather != null) {
            logger.debug("Found weather data: {}", weather);
            promise.complete(weather);
        } else {
            // Generate new weather data if none exists
            weather = generateWeatherData(city);
            weatherData.put(city, weather);
            logger.debug("Generated new weather data: {}", weather);
            promise.complete(weather);
        }
        return promise.future();
    }

    @Override
    public Future<List<Weather>> findAll() {
        logger.debug("Finding weather data for all cities");

        List<Weather> weatherList = new ArrayList<>();
        for (String city : cities) {
            Weather weather = weatherData.get(city);
            if (weather == null) {
                weather = generateWeatherData(city);
                weatherData.put(city, weather);
            }
            weatherList.add(weather);
        }
        logger.debug("Found weather data for {} cities", weatherList.size());
        return Future.succeededFuture(weatherList);
    }

    @Override
    public Future<Weather> save(Weather weather) {
        logger.debug("Saving weather data: {}", weather);

        Promise<Weather> promise = Promise.promise();
        String city = weather.getCity();
        if (city == null || city.isEmpty()) {
            logger.debug("City name is required");
            return Future.failedFuture("City name is required");
        }

        if (!cities.contains(city)) {
            cities.add(city);
            logger.debug("Added new city: {}", city);
        }

        weatherData.put(city, weather);
        return Future.succeededFuture(weather);
    }

    @Override
    public Future<List<Weather>> getForecast(String city, int days) {
        logger.debug("Getting {}-day forecast for city: {}", days, city);

        Promise<List<Weather>> promise = Promise.promise();
        if (!cities.contains(city)) {
            logger.debug("City not found: {}", city);
            promise.fail("City not found: " + city);
            return promise.future();
        }

        List<Weather> forecast = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            Weather weather = generateWeatherData(city);
            // Adjust timestamp for future days
            weather.setTimestamp(System.currentTimeMillis() + (i * 24 * 60 * 60 * 1000));
            forecast.add(weather);
        }
        logger.debug("Generated forecast with {} days", forecast.size());
        return Future.succeededFuture(forecast);
    }

    @Override
    public Future<List<String>> getCities() {
        logger.debug("Getting list of cities");
        return Future.succeededFuture(new ArrayList<>(cities));
    }

    @Override
    public Future<Void> initialize() {
        logger.info("Initializing repository with sample data");

        Promise<Void> promise = Promise.promise();

        // Clear any existing data
        weatherData.clear();
        cities.clear();

        // Add sample cities
        cities.add("New York");
        cities.add("London");
        cities.add("Paris");
        cities.add("Tokyo");
        cities.add("Sydney");
        cities.add("Berlin");
        cities.add("Moscow");
        cities.add("Beijing");
        cities.add("Rio de Janeiro");
        cities.add("Cairo");

        // Generate initial weather data for each city
        for (String city : cities) {
            weatherData.put(city, generateWeatherData(city));
        }

        logger.info("Sample data initialized with {} cities", cities.size());
        promise.complete();

        return promise.future();
    }

    @Override
    public Future<Integer> count() {
        logger.debug("Getting city count");
        return Future.succeededFuture(cities.size());
    }

    @Override
    public Future<Weather> findById(String id) {
        logger.debug("Finding weather by ID: {}", id);

        Promise<Weather> promise = Promise.promise();
        Weather weather = weatherData.get(id);
        if (weather != null) {
            logger.debug("Found weather: {}", weather);
            promise.complete(weather);
        } else {
            logger.debug("Weather not found with ID: {}", id);
            promise.fail("Weather not found with ID: " + id);
        }
        return promise.future();
    }

    @Override
    public Future<Void> deleteById(String id) {
        logger.debug("Deleting weather by ID: {}", id);

        Promise<Void> promise = Promise.promise();
        Weather removed = weatherData.remove(id);
        if (removed != null) {
            logger.debug("Deleted weather: {}", removed);
            promise.complete();
        } else {
            logger.debug("Weather not found with ID: {}", id);
            promise.fail("Weather not found with ID: " + id);
        }
        return promise.future();
    }

    /**
     * Generates random weather data for a city.
     *
     * @param city the city name
     * @return a Weather object with the generated data
     */
    private Weather generateWeatherData(String city) {
        double temperature = 10 + 30 * random.nextDouble(); // 10-40°C
        double humidity = 30 + 60 * random.nextDouble(); // 30-90%
        double windSpeed = 5 + 25 * random.nextDouble(); // 5-30 km/h
        
        String[] conditions = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Stormy", "Snowy"};
        String condition = conditions[random.nextInt(conditions.length)];
        
        return new Weather(
            city,
            Math.round(temperature * 10) / 10.0,
            Math.round(humidity * 10) / 10.0,
            Math.round(windSpeed * 10) / 10.0,
            condition,
            System.currentTimeMillis()
        );
    }
}