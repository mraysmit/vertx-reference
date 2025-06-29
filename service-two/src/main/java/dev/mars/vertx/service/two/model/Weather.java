package dev.mars.vertx.service.two.model;

import io.vertx.core.json.JsonObject;
import java.util.Objects;

/**
 * Model class representing Weather data.
 */
public class Weather {
    private String id;
    private String city;
    private double temperature;
    private double humidity;
    private double windSpeed;
    private String condition;
    private long timestamp;

    /**
     * Default constructor.
     */
    public Weather() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id the unique identifier
     * @param city the city name
     * @param temperature the temperature in Celsius
     * @param humidity the humidity percentage
     * @param windSpeed the wind speed in km/h
     * @param condition the weather condition
     * @param timestamp the timestamp when the weather data was generated
     */
    public Weather(String id, String city, double temperature, double humidity, double windSpeed, String condition, long timestamp) {
        this.id = id;
        this.city = city;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.timestamp = timestamp;
    }

    /**
     * Constructor with all fields except ID (ID will be generated).
     *
     * @param city the city name
     * @param temperature the temperature in Celsius
     * @param humidity the humidity percentage
     * @param windSpeed the wind speed in km/h
     * @param condition the weather condition
     * @param timestamp the timestamp when the weather data was generated
     */
    public Weather(String city, double temperature, double humidity, double windSpeed, String condition, long timestamp) {
        this.id = generateId();
        this.city = city;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.condition = condition;
        this.timestamp = timestamp;
    }

    /**
     * Creates a Weather object from a JsonObject.
     *
     * @param json the JsonObject
     * @return the Weather object
     */
    public static Weather fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        return new Weather(
            json.getString("id"),
            json.getString("city"),
            json.getDouble("temperature", 0.0),
            json.getDouble("humidity", 0.0),
            json.getDouble("windSpeed", 0.0),
            json.getString("condition", "Unknown"),
            json.getLong("timestamp", System.currentTimeMillis())
        );
    }

    /**
     * Converts this Weather object to a JsonObject.
     *
     * @return the JsonObject
     */
    public JsonObject toJson() {
        return new JsonObject()
            .put("id", id)
            .put("city", city)
            .put("temperature", temperature)
            .put("humidity", humidity)
            .put("windSpeed", windSpeed)
            .put("condition", condition)
            .put("timestamp", timestamp);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Generates a unique ID for the weather data.
     *
     * @return a unique ID
     */
    private String generateId() {
        return "weather-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Weather weather = (Weather) o;
        return Objects.equals(id, weather.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Weather{" +
                "id='" + id + '\'' +
                ", city='" + city + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", condition='" + condition + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}