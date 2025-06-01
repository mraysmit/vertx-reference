package dev.mars.vertx.service.two;

import dev.mars.vertx.common.eventbus.EventBusService;
import dev.mars.vertx.common.util.ExceptionHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service Two Verticle.
 * Handles requests from the API Gateway via the event bus.
 * Demonstrates best practices for Vert.x verticles.
 * This service simulates a weather data service.
 */
public class ServiceTwoVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(ServiceTwoVerticle.class);
    
    private EventBusService eventBusService;
    private MessageConsumer<JsonObject> consumer;
    
    // Simulated weather data
    private final List<String> cities = new ArrayList<>();
    private final Random random = new Random();
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    
    @Override
    public void start(Promise<Void> startPromise) {
        logger.info("Starting Service Two Verticle");
        
        // Initialize services
        eventBusService = new EventBusService(vertx);
        
        // Initialize sample data
        initializeSampleData();
        
        // Register event bus consumer
        String serviceAddress = config().getString("service.address", "service.two");
        
        consumer = eventBusService.consumer(serviceAddress, this::handleRequest);
        
        // Complete the start
        startPromise.complete();
        logger.info("Service Two Verticle started successfully");
    }
    
    @Override
    public void stop(Promise<Void> stopPromise) {
        logger.info("Stopping Service Two Verticle");
        
        // Unregister event bus consumer
        if (consumer != null) {
            eventBusService.unregisterConsumer(consumer)
                .onSuccess(v -> {
                    logger.info("Event bus consumer unregistered successfully");
                    stopPromise.complete();
                })
                .onFailure(err -> {
                    logger.error("Failed to unregister event bus consumer", err);
                    stopPromise.fail(err);
                });
        } else {
            stopPromise.complete();
        }
    }
    
    /**
     * Handles requests from the API Gateway.
     * 
     * @param request the request from the API Gateway
     * @return a Future with the response
     */
    private Future<Object> handleRequest(JsonObject request) {
        logger.info("Received request: {}", request);
        
        // Increment request counter
        int count = requestCounter.incrementAndGet();
        
        // Simulate occasional failures for demonstration purposes
        if (count % 10 == 0) {
            logger.warn("Simulating a failure for request #{}", count);
            return Future.failedFuture("Simulated failure for demonstration purposes");
        }
        
        try {
            // Check if the request has a city
            if (request.containsKey("city")) {
                String city = request.getString("city");
                return getWeatherForCity(city);
            } else if (request.containsKey("action")) {
                String action = request.getString("action");
                
                switch (action) {
                    case "forecast":
                        return getForecast(request);
                    case "cities":
                        return listCities();
                    case "stats":
                        return getStats();
                    default:
                        return Future.failedFuture("Unknown action: " + action);
                }
            } else {
                // Default to returning weather for a random city
                return getRandomWeather();
            }
        } catch (Exception e) {
            return ExceptionHandler.handleException(e, "Error processing request");
        }
    }
    
    /**
     * Gets weather data for a specific city.
     * 
     * @param city the city name
     * @return a Future with the weather data
     */
    private Future<Object> getWeatherForCity(String city) {
        logger.info("Getting weather for city: {}", city);
        
        return vertx.executeBlocking(promise -> {
            try {
                // Simulate a delay
                Thread.sleep(100);
                
                if (!cities.contains(city)) {
                    promise.fail("City not found: " + city);
                    return;
                }
                
                JsonObject weather = generateWeatherData(city);
                promise.complete(weather);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                promise.fail(e);
            }
        });
    }
    
    /**
     * Gets a weather forecast for a city.
     * 
     * @param request the request containing the city and days
     * @return a Future with the forecast data
     */
    private Future<Object> getForecast(JsonObject request) {
        String city = request.getString("city");
        int days = request.getInteger("days", 5);
        
        logger.info("Getting {}-day forecast for city: {}", days, city);
        
        return vertx.executeBlocking(promise -> {
            try {
                // Simulate a delay
                Thread.sleep(200);
                
                if (!cities.contains(city)) {
                    promise.fail("City not found: " + city);
                    return;
                }
                
                JsonObject forecast = new JsonObject()
                        .put("city", city)
                        .put("days", days);
                
                List<JsonObject> dailyForecasts = new ArrayList<>();
                for (int i = 0; i < days; i++) {
                    JsonObject daily = generateWeatherData(city);
                    daily.put("day", i + 1);
                    dailyForecasts.add(daily);
                }
                
                forecast.put("forecast", dailyForecasts);
                promise.complete(forecast);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                promise.fail(e);
            }
        });
    }
    
    /**
     * Gets weather data for a random city.
     * 
     * @return a Future with the weather data
     */
    private Future<Object> getRandomWeather() {
        String city = cities.get(random.nextInt(cities.size()));
        logger.info("Getting weather for random city: {}", city);
        
        return getWeatherForCity(city);
    }
    
    /**
     * Lists all available cities.
     * 
     * @return a Future with the list of cities
     */
    private Future<Object> listCities() {
        logger.info("Listing all cities");
        
        return vertx.executeBlocking(promise -> {
            try {
                // Simulate a delay
                Thread.sleep(50);
                
                JsonObject result = new JsonObject()
                        .put("cities", cities)
                        .put("count", cities.size());
                
                promise.complete(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                promise.fail(e);
            }
        });
    }
    
    /**
     * Gets statistics about the service.
     * 
     * @return a Future with the statistics
     */
    private Future<Object> getStats() {
        logger.info("Getting service statistics");
        
        return vertx.executeBlocking(promise -> {
            try {
                // Simulate a delay
                Thread.sleep(30);
                
                JsonObject stats = new JsonObject()
                        .put("requestsProcessed", requestCounter.get())
                        .put("citiesAvailable", cities.size())
                        .put("uptime", System.currentTimeMillis() - vertx.getOrCreateContext().deploymentID().hashCode());
                
                promise.complete(stats);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                promise.fail(e);
            }
        });
    }
    
    /**
     * Generates random weather data for a city.
     * 
     * @param city the city name
     * @return a JsonObject with the weather data
     */
    private JsonObject generateWeatherData(String city) {
        double temperature = 10 + 30 * random.nextDouble(); // 10-40°C
        double humidity = 30 + 60 * random.nextDouble(); // 30-90%
        double windSpeed = 5 + 25 * random.nextDouble(); // 5-30 km/h
        
        String[] conditions = {"Sunny", "Partly Cloudy", "Cloudy", "Rainy", "Stormy", "Snowy"};
        String condition = conditions[random.nextInt(conditions.length)];
        
        return new JsonObject()
                .put("city", city)
                .put("temperature", Math.round(temperature * 10) / 10.0)
                .put("humidity", Math.round(humidity * 10) / 10.0)
                .put("windSpeed", Math.round(windSpeed * 10) / 10.0)
                .put("condition", condition)
                .put("timestamp", System.currentTimeMillis());
    }
    
    /**
     * Initializes sample data.
     */
    private void initializeSampleData() {
        logger.info("Initializing sample data");
        
        // Add some cities
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
        
        logger.info("Sample data initialized with {} cities", cities.size());
    }
}