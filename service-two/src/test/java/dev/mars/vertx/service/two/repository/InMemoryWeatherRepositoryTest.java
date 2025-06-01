package dev.mars.vertx.service.two.repository;

import dev.mars.vertx.service.two.model.Weather;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(VertxExtension.class)
class InMemoryWeatherRepositoryTest {

    private Vertx vertx;
    private WeatherRepository repository;

    @BeforeEach
    void setUp(VertxTestContext testContext) {
        vertx = Vertx.vertx();
        repository = new InMemoryWeatherRepository();

        // Initialize the repository with sample data
        repository.initialize()
            .onComplete(testContext.succeedingThenComplete());
    }

    @AfterEach
    void tearDown(VertxTestContext testContext) {
        vertx.close(testContext.succeedingThenComplete());
    }

    @Test
    void testInitialize(VertxTestContext testContext) {
        repository.count()
            .onComplete(testContext.succeeding(count -> {
                testContext.verify(() -> {
                    assertEquals(10, count);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testFindByCity(VertxTestContext testContext) {
        repository.findByCity("London")
            .onComplete(testContext.succeeding(weather -> {
                testContext.verify(() -> {
                    assertNotNull(weather);
                    assertEquals("London", weather.getCity());
                    assertTrue(weather.getTemperature() >= 10 && weather.getTemperature() <= 40);
                    assertTrue(weather.getHumidity() >= 30 && weather.getHumidity() <= 90);
                    assertTrue(weather.getWindSpeed() >= 5 && weather.getWindSpeed() <= 30);
                    assertNotNull(weather.getCondition());
                    assertTrue(weather.getTimestamp() > 0);
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testFindByCityNotFound(VertxTestContext testContext) {
        repository.findByCity("non-existent-city")
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testFindAll(VertxTestContext testContext) {
        repository.findAll()
            .onComplete(testContext.succeeding(weatherList -> {
                testContext.verify(() -> {
                    assertNotNull(weatherList);
                    assertEquals(10, weatherList.size());

                    // Verify that all expected cities are present
                    boolean hasLondon = false;
                    boolean hasTokyo = false;

                    for (Weather weather : weatherList) {
                        if ("London".equals(weather.getCity())) {
                            hasLondon = true;
                        } else if ("Tokyo".equals(weather.getCity())) {
                            hasTokyo = true;
                        }
                    }

                    assertTrue(hasLondon, "London should be present");
                    assertTrue(hasTokyo, "Tokyo should be present");

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testSaveNewWeather(VertxTestContext testContext) {
        Weather newWeather = new Weather("Miami", 30.5, 75.0, 15.0, "Sunny", System.currentTimeMillis());

        repository.save(newWeather)
            .compose(savedWeather -> {
                // Verify the saved weather
                assertEquals("Miami", savedWeather.getCity());
                assertEquals(30.5, savedWeather.getTemperature());
                assertEquals(75.0, savedWeather.getHumidity());
                assertEquals(15.0, savedWeather.getWindSpeed());
                assertEquals("Sunny", savedWeather.getCondition());

                // Now try to find the weather by city
                return repository.findByCity("Miami");
            })
            .onComplete(testContext.succeeding(foundWeather -> {
                testContext.verify(() -> {
                    assertNotNull(foundWeather);
                    assertEquals("Miami", foundWeather.getCity());
                    assertEquals(30.5, foundWeather.getTemperature());
                    assertEquals(75.0, foundWeather.getHumidity());
                    assertEquals(15.0, foundWeather.getWindSpeed());
                    assertEquals("Sunny", foundWeather.getCondition());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testSaveExistingWeather(VertxTestContext testContext) {
        repository.findByCity("London")
            .compose(weather -> {
                // Update the weather
                weather.setTemperature(25.0);
                weather.setCondition("Rainy");
                return repository.save(weather);
            })
            .compose(updatedWeather -> {
                // Verify the updated weather
                assertEquals("London", updatedWeather.getCity());
                assertEquals(25.0, updatedWeather.getTemperature());
                assertEquals("Rainy", updatedWeather.getCondition());

                // Now try to find the weather by city
                return repository.findByCity("London");
            })
            .onComplete(testContext.succeeding(foundWeather -> {
                testContext.verify(() -> {
                    assertNotNull(foundWeather);
                    assertEquals("London", foundWeather.getCity());
                    assertEquals(25.0, foundWeather.getTemperature());
                    assertEquals("Rainy", foundWeather.getCondition());
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testSaveWeatherWithoutCity(VertxTestContext testContext) {
        Weather invalidWeather = new Weather(null, 30.5, 75.0, 15.0, "Sunny", System.currentTimeMillis());

        repository.save(invalidWeather)
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("City name is required"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetForecast(VertxTestContext testContext) {
        repository.getForecast("Paris", 5)
            .onComplete(testContext.succeeding(forecast -> {
                testContext.verify(() -> {
                    assertNotNull(forecast);
                    assertEquals(5, forecast.size());

                    // Verify that all forecast items are for Paris
                    for (Weather weather : forecast) {
                        assertEquals("Paris", weather.getCity());
                        assertTrue(weather.getTemperature() >= 10 && weather.getTemperature() <= 40);
                        assertTrue(weather.getHumidity() >= 30 && weather.getHumidity() <= 90);
                        assertTrue(weather.getWindSpeed() >= 5 && weather.getWindSpeed() <= 30);
                        assertNotNull(weather.getCondition());
                        assertTrue(weather.getTimestamp() > 0);
                    }

                    // Verify that timestamps are in ascending order
                    for (int i = 1; i < forecast.size(); i++) {
                        assertTrue(forecast.get(i).getTimestamp() > forecast.get(i-1).getTimestamp());
                    }

                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetForecastCityNotFound(VertxTestContext testContext) {
        repository.getForecast("non-existent-city", 5)
            .onComplete(testContext.failing(err -> {
                testContext.verify(() -> {
                    assertTrue(err.getMessage().contains("not found"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testGetCities(VertxTestContext testContext) {
        repository.getCities()
            .onComplete(testContext.succeeding(cities -> {
                testContext.verify(() -> {
                    assertNotNull(cities);
                    assertEquals(10, cities.size());
                    assertTrue(cities.contains("London"));
                    assertTrue(cities.contains("New York"));
                    assertTrue(cities.contains("Tokyo"));
                    testContext.completeNow();
                });
            }));
    }

    @Test
    void testCount(VertxTestContext testContext) {
        repository.count()
            .onComplete(testContext.succeeding(count -> {
                testContext.verify(() -> {
                    assertEquals(10, count);
                    testContext.completeNow();
                });
            }));
    }
}