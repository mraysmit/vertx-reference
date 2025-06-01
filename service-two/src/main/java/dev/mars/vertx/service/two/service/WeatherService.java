package dev.mars.vertx.service.two.service;

import dev.mars.vertx.service.two.model.Weather;
import dev.mars.vertx.service.two.repository.WeatherRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service class for Weather operations.
 * Contains business logic for handling weather data.
 */
public class WeatherService {
    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherRepository weatherRepository;
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    /**
     * Constructor.
     *
     * @param weatherRepository the weather repository
     */
    public WeatherService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    /**
     * Initializes the service with sample data.
     *
     * @return a Future that completes when initialization is done
     */
    public Future<Void> initialize() {
        logger.info("Initializing weather service");
        return weatherRepository.initialize();
    }

    /**
     * Gets weather data for a specific city.
     *
     * @param city the city name
     * @return a Future with the weather data
     */
    public Future<Weather> getWeatherForCity(String city) {
        logger.info("Getting weather for city: {}", city);
        incrementRequestCounter();
        return weatherRepository.findByCity(city);
    }

    /**
     * Gets weather data for a random city.
     *
     * @return a Future with the weather data
     */
    public Future<Weather> getRandomWeather() {
        logger.info("Getting weather for a random city");
        incrementRequestCounter();

        return weatherRepository.getCities()
            .compose(cities -> {
                if (cities.isEmpty()) {
                    return Future.failedFuture("No cities available");
                }

                int randomIndex = (int) (Math.random() * cities.size());
                String randomCity = cities.get(randomIndex);
                logger.debug("Selected random city: {}", randomCity);

                return weatherRepository.findByCity(randomCity);
            });
    }

    /**
     * Gets a forecast for a city.
     *
     * @param city the city name
     * @param days the number of days for the forecast
     * @return a Future with the forecast data
     */
    public Future<JsonObject> getForecast(String city, int days) {
        logger.info("Getting {}-day forecast for city: {}", days, city);
        incrementRequestCounter();

        return weatherRepository.getForecast(city, days)
            .compose(forecast -> {
                JsonObject result = new JsonObject()
                    .put("city", city)
                    .put("days", days)
                    .put("forecast", new io.vertx.core.json.JsonArray(forecast.stream().map(Weather::toJson).toList()));

                return Future.succeededFuture(result);
            });
    }

    /**
     * Lists all available cities.
     *
     * @return a Future with the list of cities
     */
    public Future<JsonObject> listCities() {
        logger.info("Listing all cities");
        incrementRequestCounter();

        return weatherRepository.getCities()
            .compose(cities -> {
                JsonObject result = new JsonObject()
                    .put("cities", cities)
                    .put("count", cities.size());

                return Future.succeededFuture(result);
            });
    }

    /**
     * Gets statistics about the service.
     *
     * @return a Future with the statistics
     */
    public Future<JsonObject> getStats() {
        logger.info("Getting service statistics");
        incrementRequestCounter();

        return weatherRepository.count()
            .compose(count -> {
                JsonObject stats = new JsonObject()
                    .put("requestsProcessed", requestCounter.get())
                    .put("citiesAvailable", count)
                    .put("uptime", System.currentTimeMillis());

                return Future.succeededFuture(stats);
            });
    }

    /**
     * Increments the request counter.
     *
     * @return the new count
     */
    private int incrementRequestCounter() {
        return requestCounter.incrementAndGet();
    }
}
