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
     * Gets an item by ID.
     *
     * @param id the item ID
     * @return a Future with the item as a JsonObject
     */
    public Future<JsonObject> getItem(String id) {
        logger.info("Getting item with ID: {}", id);
        incrementRequestCounter();

        return weatherRepository.findById(id)
            .map(Weather::toJson)
            .recover(err -> {
                logger.warn("Item not found with ID: {}", id);
                return Future.failedFuture("Item not found with ID: " + id);
            });
    }

    /**
     * Lists all items.
     *
     * @return a Future with the list of items as a JsonObject
     */
    public Future<JsonObject> listItems() {
        logger.info("Listing all items");
        incrementRequestCounter();

        return weatherRepository.findAll()
            .map(weatherList -> {
                JsonObject result = new JsonObject()
                    .put("items", weatherList.stream().map(Weather::toJson).toArray())
                    .put("count", weatherList.size());

                return result;
            });
    }

    /**
     * Creates a new item.
     *
     * @param itemData the item data
     * @return a Future with the created item as a JsonObject
     */
    public Future<JsonObject> createItem(JsonObject itemData) {
        logger.info("Creating new item: {}", itemData);
        incrementRequestCounter();

        Weather weather = new Weather();
        weather.setCity(itemData.getString("city", "Unknown"));
        weather.setTemperature(itemData.getDouble("temperature", 20.0));
        weather.setDescription(itemData.getString("description", "Clear"));
        weather.setHumidity(itemData.getInteger("humidity", 50));
        weather.setWindSpeed(itemData.getDouble("windSpeed", 5.0));

        return weatherRepository.save(weather)
            .map(Weather::toJson);
    }

    /**
     * Updates an existing item.
     *
     * @param itemData the item data containing the ID and updated fields
     * @return a Future with the updated item as a JsonObject
     */
    public Future<JsonObject> updateItem(JsonObject itemData) {
        String id = itemData.getString("id");
        if (id == null) {
            return Future.failedFuture("ID is required for update");
        }

        logger.info("Updating item with ID: {}", id);
        incrementRequestCounter();

        return weatherRepository.findById(id)
            .compose(weather -> {
                // Update fields if provided
                if (itemData.containsKey("city")) {
                    weather.setCity(itemData.getString("city"));
                }
                if (itemData.containsKey("temperature")) {
                    weather.setTemperature(itemData.getDouble("temperature"));
                }
                if (itemData.containsKey("description")) {
                    weather.setDescription(itemData.getString("description"));
                }
                if (itemData.containsKey("humidity")) {
                    weather.setHumidity(itemData.getInteger("humidity"));
                }
                if (itemData.containsKey("windSpeed")) {
                    weather.setWindSpeed(itemData.getDouble("windSpeed"));
                }

                return weatherRepository.save(weather);
            })
            .map(Weather::toJson)
            .recover(err -> {
                logger.warn("Failed to update item with ID: {}", id);
                return Future.failedFuture("Item not found with ID: " + id);
            });
    }

    /**
     * Deletes an item by ID.
     *
     * @param id the item ID
     * @return a Future with the deletion result as a JsonObject
     */
    public Future<JsonObject> deleteItem(String id) {
        logger.info("Deleting item with ID: {}", id);
        incrementRequestCounter();

        return weatherRepository.deleteById(id)
            .map(v -> new JsonObject()
                .put("success", true)
                .put("message", "Item deleted successfully")
                .put("id", id))
            .recover(err -> {
                logger.warn("Failed to delete item with ID: {}", id);
                return Future.failedFuture("Item not found with ID: " + id);
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
