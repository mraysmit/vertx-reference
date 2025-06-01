package dev.mars.vertx.service.two.model;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WeatherTest {

    @Test
    void testDefaultConstructor() {
        Weather weather = new Weather();
        assertNull(weather.getCity());
        assertEquals(0.0, weather.getTemperature());
        assertEquals(0.0, weather.getHumidity());
        assertEquals(0.0, weather.getWindSpeed());
        assertNull(weather.getCondition());
        assertEquals(0, weather.getTimestamp());
    }

    @Test
    void testParameterizedConstructor() {
        String city = "London";
        double temperature = 20.5;
        double humidity = 65.0;
        double windSpeed = 10.2;
        String condition = "Sunny";
        long timestamp = System.currentTimeMillis();

        Weather weather = new Weather(city, temperature, humidity, windSpeed, condition, timestamp);
        
        assertEquals(city, weather.getCity());
        assertEquals(temperature, weather.getTemperature());
        assertEquals(humidity, weather.getHumidity());
        assertEquals(windSpeed, weather.getWindSpeed());
        assertEquals(condition, weather.getCondition());
        assertEquals(timestamp, weather.getTimestamp());
    }

    @Test
    void testFromJson() {
        String city = "London";
        double temperature = 20.5;
        double humidity = 65.0;
        double windSpeed = 10.2;
        String condition = "Sunny";
        long timestamp = System.currentTimeMillis();

        JsonObject json = new JsonObject()
                .put("city", city)
                .put("temperature", temperature)
                .put("humidity", humidity)
                .put("windSpeed", windSpeed)
                .put("condition", condition)
                .put("timestamp", timestamp);

        Weather weather = Weather.fromJson(json);
        
        assertEquals(city, weather.getCity());
        assertEquals(temperature, weather.getTemperature());
        assertEquals(humidity, weather.getHumidity());
        assertEquals(windSpeed, weather.getWindSpeed());
        assertEquals(condition, weather.getCondition());
        assertEquals(timestamp, weather.getTimestamp());
    }

    @Test
    void testFromJsonWithDefaults() {
        String city = "London";
        
        JsonObject json = new JsonObject()
                .put("city", city);

        Weather weather = Weather.fromJson(json);
        
        assertEquals(city, weather.getCity());
        assertEquals(0.0, weather.getTemperature());
        assertEquals(0.0, weather.getHumidity());
        assertEquals(0.0, weather.getWindSpeed());
        assertEquals("Unknown", weather.getCondition());
        assertTrue(weather.getTimestamp() > 0);
    }

    @Test
    void testFromJsonNull() {
        assertNull(Weather.fromJson(null));
    }

    @Test
    void testToJson() {
        String city = "London";
        double temperature = 20.5;
        double humidity = 65.0;
        double windSpeed = 10.2;
        String condition = "Sunny";
        long timestamp = System.currentTimeMillis();

        Weather weather = new Weather(city, temperature, humidity, windSpeed, condition, timestamp);
        JsonObject json = weather.toJson();
        
        assertEquals(city, json.getString("city"));
        assertEquals(temperature, json.getDouble("temperature"));
        assertEquals(humidity, json.getDouble("humidity"));
        assertEquals(windSpeed, json.getDouble("windSpeed"));
        assertEquals(condition, json.getString("condition"));
        assertEquals(timestamp, json.getLong("timestamp"));
    }

    @Test
    void testSettersAndGetters() {
        Weather weather = new Weather();
        
        String city = "London";
        double temperature = 20.5;
        double humidity = 65.0;
        double windSpeed = 10.2;
        String condition = "Sunny";
        long timestamp = System.currentTimeMillis();
        
        weather.setCity(city);
        weather.setTemperature(temperature);
        weather.setHumidity(humidity);
        weather.setWindSpeed(windSpeed);
        weather.setCondition(condition);
        weather.setTimestamp(timestamp);
        
        assertEquals(city, weather.getCity());
        assertEquals(temperature, weather.getTemperature());
        assertEquals(humidity, weather.getHumidity());
        assertEquals(windSpeed, weather.getWindSpeed());
        assertEquals(condition, weather.getCondition());
        assertEquals(timestamp, weather.getTimestamp());
    }

    @Test
    void testEquals() {
        Weather weather1 = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 1000L);
        Weather weather2 = new Weather("London", 25.0, 70.0, 15.0, "Cloudy", 1000L);
        Weather weather3 = new Weather("Paris", 20.5, 65.0, 10.2, "Sunny", 1000L);
        Weather weather4 = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 2000L);
        
        assertEquals(weather1, weather1);
        assertEquals(weather1, weather2); // Same city and timestamp, different other fields
        assertNotEquals(weather1, weather3); // Different city
        assertNotEquals(weather1, weather4); // Different timestamp
        assertNotEquals(weather1, null);
        assertNotEquals(weather1, "not a weather");
    }

    @Test
    void testHashCode() {
        Weather weather1 = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 1000L);
        Weather weather2 = new Weather("London", 25.0, 70.0, 15.0, "Cloudy", 1000L);
        Weather weather3 = new Weather("Paris", 20.5, 65.0, 10.2, "Sunny", 1000L);
        Weather weather4 = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 2000L);
        
        assertEquals(weather1.hashCode(), weather2.hashCode()); // Same city and timestamp
        assertNotEquals(weather1.hashCode(), weather3.hashCode()); // Different city
        assertNotEquals(weather1.hashCode(), weather4.hashCode()); // Different timestamp
    }

    @Test
    void testToString() {
        Weather weather = new Weather("London", 20.5, 65.0, 10.2, "Sunny", 1000L);
        String toString = weather.toString();
        
        assertTrue(toString.contains("London"));
        assertTrue(toString.contains("20.5"));
        assertTrue(toString.contains("65.0"));
        assertTrue(toString.contains("10.2"));
        assertTrue(toString.contains("Sunny"));
        assertTrue(toString.contains("1000"));
    }
}