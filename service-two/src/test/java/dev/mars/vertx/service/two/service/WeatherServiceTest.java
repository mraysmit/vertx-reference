package dev.mars.vertx.service.two.service;

import dev.mars.vertx.service.two.model.Weather;
import dev.mars.vertx.service.two.repository.WeatherRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class WeatherServiceTest {

    private MockWeatherRepository mockRepository;
    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        mockRepository = new MockWeatherRepository();
        weatherService = new WeatherService(mockRepository);
    }

    @Test
    void testInitialize(VertxTestContext testContext) {
        weatherService.initialize()
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertTrue(mockRepository.isInitialized());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetWeatherForCity(VertxTestContext testContext) {
        // Add a test weather to the repository
        Weather testWeather = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 1000L);
        mockRepository.addWeather(testWeather);

        // Call the service method
        weatherService.getWeatherForCity("London")
            .onComplete(testContext.succeeding(weather -> {
                testContext.verify(() -> {
                    assertEquals("London", weather.getCity());
                    assertEquals(20.5, weather.getTemperature());
                    assertEquals(65.0, weather.getHumidity());
                    assertEquals(10.2, weather.getWindSpeed());
                    assertEquals("Sunny", weather.getCondition());
                    assertEquals(1000L, weather.getTimestamp());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetWeatherForCityNotFound(VertxTestContext testContext) {
        // Call the service method with a non-existent city
        weatherService.getWeatherForCity("non-existent-city")
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetRandomWeather(VertxTestContext testContext) {
        // Add test cities and weather to the repository
        List<String> cities = Arrays.asList("London", "Paris", "New York");
        mockRepository.setCities(cities);

        // Add weather for all cities
        Weather londonWeather = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 1000L);
        Weather parisWeather = new Weather("Paris", 22.0, 60.0, 8.5, "Cloudy", 2000L);
        Weather newYorkWeather = new Weather("New York", 25.0, 55.0, 12.0, "Rainy", 3000L);

        mockRepository.addWeather(londonWeather);
        mockRepository.addWeather(parisWeather);
        mockRepository.addWeather(newYorkWeather);

        // Call the service method
        weatherService.getRandomWeather()
            .onComplete(testContext.succeeding(weather -> {
                testContext.verify(() -> {
                    // Get the city from the weather object
                    String city = weather.getCity();

                    // Verify that the city is one of the expected cities
                    assertTrue(Arrays.asList("London", "Paris", "New York").contains(city), 
                            "City should be one of: London, Paris, New York");

                    // Verify the weather properties based on the city
                    if ("London".equals(city)) {
                        assertEquals(20.5, weather.getTemperature());
                        assertEquals(65.0, weather.getHumidity());
                        assertEquals(10.2, weather.getWindSpeed());
                        assertEquals("Sunny", weather.getCondition());
                        assertEquals(1000L, weather.getTimestamp());
                    } else if ("Paris".equals(city)) {
                        assertEquals(22.0, weather.getTemperature());
                        assertEquals(60.0, weather.getHumidity());
                        assertEquals(8.5, weather.getWindSpeed());
                        assertEquals("Cloudy", weather.getCondition());
                        assertEquals(2000L, weather.getTimestamp());
                    } else if ("New York".equals(city)) {
                        assertEquals(25.0, weather.getTemperature());
                        assertEquals(55.0, weather.getHumidity());
                        assertEquals(12.0, weather.getWindSpeed());
                        assertEquals("Rainy", weather.getCondition());
                        assertEquals(3000L, weather.getTimestamp());
                    }

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetRandomWeatherNoCities(VertxTestContext testContext) {
        // Set empty cities list
        mockRepository.setCities(new ArrayList<>());

        // Call the service method
        weatherService.getRandomWeather()
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("No cities available"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetForecast(VertxTestContext testContext) {
        // Add a test city and forecast to the repository
        String city = "London";
        int days = 5;

        // Add the city to the cities list
        mockRepository.setCities(Arrays.asList(city));

        List<Weather> forecast = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            forecast.add(new Weather(city, 20.0 + i, 65.0, 10.0, "Sunny", 1000L + i * 86400000L));
        }
        mockRepository.setForecast(city, days, forecast);

        // Call the service method
        weatherService.getForecast(city, days)
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertEquals(city, result.getString("city"));
                    assertEquals(days, result.getInteger("days"));
                    assertTrue(result.containsKey("forecast"));
                    assertEquals(days, result.getJsonArray("forecast").size());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetForecastCityNotFound(VertxTestContext testContext) {
        // Call the service method with a non-existent city
        weatherService.getForecast("non-existent-city", 5)
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testListCities(VertxTestContext testContext) {
        // Add test cities to the repository
        List<String> cities = Arrays.asList("London", "Paris", "New York");
        mockRepository.setCities(cities);

        // Call the service method
        weatherService.listCities()
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertTrue(result.containsKey("cities"));
                    assertTrue(result.containsKey("count"));
                    assertEquals(3, result.getInteger("count"));
                    assertEquals(3, result.getJsonArray("cities").size());
                    assertTrue(result.getJsonArray("cities").contains("London"));
                    assertTrue(result.getJsonArray("cities").contains("Paris"));
                    assertTrue(result.getJsonArray("cities").contains("New York"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetStats(VertxTestContext testContext) {
        // Set city count in repository
        mockRepository.setCityCount(5);

        // Call the service method
        weatherService.getStats()
            .onComplete(testContext.succeeding(result -> {
                testContext.verify(() -> {
                    assertTrue(result.containsKey("requestsProcessed"));
                    assertTrue(result.containsKey("citiesAvailable"));
                    assertTrue(result.containsKey("uptime"));
                    assertEquals(5, result.getInteger("citiesAvailable"));
                    // Request counter should be 1 after this call
                    assertEquals(1, result.getInteger("requestsProcessed"));
                    testContext.completeNow();
                });
            }));
    }

    /**
     * Mock implementation of WeatherRepository for testing.
     */
    private static class MockWeatherRepository implements WeatherRepository {
        private final Map<String, Weather> weatherData = new HashMap<>();
        private final Map<String, Map<Integer, List<Weather>>> forecastData = new HashMap<>();
        private List<String> cities = new ArrayList<>();
        private boolean initialized = false;
        private int cityCount = 0;

        public boolean isInitialized() {
            return initialized;
        }

        public void addWeather(Weather weather) {
            weatherData.put(weather.getCity(), weather);
            if (!cities.contains(weather.getCity())) {
                cities.add(weather.getCity());
            }
        }

        public void setCities(List<String> cities) {
            this.cities = cities;
        }

        public void setForecast(String city, int days, List<Weather> forecast) {
            forecastData.computeIfAbsent(city, k -> new HashMap<>()).put(days, forecast);
        }

        public void setCityCount(int count) {
            this.cityCount = count;
        }

        @Override
        public Future<Weather> findByCity(String city) {
            Weather weather = weatherData.get(city);
            if (weather != null) {
                return Future.succeededFuture(weather);
            } else {
                return Future.failedFuture("Weather not found for city: " + city);
            }
        }

        @Override
        public Future<List<Weather>> findAll() {
            return Future.succeededFuture(new ArrayList<>(weatherData.values()));
        }

        @Override
        public Future<Weather> save(Weather weather) {
            weatherData.put(weather.getCity(), weather);
            if (!cities.contains(weather.getCity())) {
                cities.add(weather.getCity());
            }
            return Future.succeededFuture(weather);
        }

        @Override
        public Future<List<Weather>> getForecast(String city, int days) {
            if (!cities.contains(city)) {
                return Future.failedFuture("City not found: " + city);
            }

            Map<Integer, List<Weather>> cityForecasts = forecastData.get(city);
            if (cityForecasts != null && cityForecasts.containsKey(days)) {
                return Future.succeededFuture(cityForecasts.get(days));
            }

            // Generate a default forecast if none is set
            List<Weather> forecast = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                forecast.add(new Weather(city, 20.0 + i, 65.0, 10.0, "Sunny", 1000L + i * 86400000L));
            }
            return Future.succeededFuture(forecast);
        }

        @Override
        public Future<List<String>> getCities() {
            return Future.succeededFuture(cities);
        }

        @Override
        public Future<Void> initialize() {
            initialized = true;
            return Future.succeededFuture();
        }

        @Override
        public Future<Integer> count() {
            return Future.succeededFuture(cityCount > 0 ? cityCount : cities.size());
        }
    }
}
