# Implementation of Public API for Service-Two's Specialized Operations

To expose the specialized operations in service-two's WeatherHandler through the public API, I need to make changes to three key components:

1. Add new endpoint definitions to the OpenAPI specification
2. Register new operation handlers in the RouterFactory
3. Add new routes in the RouterFactory

## 1. Changes to OpenAPI Specification (openapi.yaml)

Add the following new endpoint definitions to the OpenAPI specification after the existing service-two endpoints:

```yaml
  /api/service-two/random:
    get:
      summary: Get weather for a random city
      description: Returns weather data for a randomly selected city
      operationId: getServiceTwoRandomWeather
      tags:
        - Service Two
      responses:
        '200':
          description: Weather data retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ServiceTwoItem'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/service-two/forecast/{city}:
    get:
      summary: Get weather forecast for a city
      description: Returns a multi-day weather forecast for the specified city
      operationId: getServiceTwoForecast
      tags:
        - Service Two
      parameters:
        - name: city
          in: path
          required: true
          description: Name of the city
          schema:
            type: string
        - name: days
          in: query
          required: false
          description: Number of days for the forecast (default is 5)
          schema:
            type: integer
            default: 5
      responses:
        '200':
          description: Forecast retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  city:
                    type: string
                    description: City name
                  days:
                    type: integer
                    description: Number of days in the forecast
                  forecast:
                    type: array
                    items:
                      $ref: '#/components/schemas/ServiceTwoItem'
        '404':
          description: City not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/service-two/cities:
    get:
      summary: List all available cities
      description: Returns a list of all cities for which weather data is available
      operationId: getServiceTwoCities
      tags:
        - Service Two
      responses:
        '200':
          description: Cities retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  cities:
                    type: array
                    items:
                      type: string
                  count:
                    type: integer
                    description: Number of cities
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /api/service-two/stats:
    get:
      summary: Get service statistics
      description: Returns statistics about the weather service
      operationId: getServiceTwoStats
      tags:
        - Service Two
      responses:
        '200':
          description: Statistics retrieved successfully
          content:
            application/json:
              schema:
                type: object
                properties:
                  requestsProcessed:
                    type: integer
                    description: Number of requests processed
                  citiesAvailable:
                    type: integer
                    description: Number of cities available
                  uptime:
                    type: integer
                    format: int64
                    description: Service uptime in milliseconds
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

## 2. Changes to RouterFactory.java

Update the `createRouter` method to register the new operation handlers:

```java
// Service Two handlers
handlers.put("getServiceTwoItem", serviceTwoHandler);
handlers.put("createServiceTwoItem", serviceTwoHandler);
handlers.put("getServiceTwoRandomWeather", serviceTwoHandler);
handlers.put("getServiceTwoForecast", serviceTwoHandler);
handlers.put("getServiceTwoCities", serviceTwoHandler);
handlers.put("getServiceTwoStats", serviceTwoHandler);
```

Update the `createRouterFromOpenAPI` method to add the new routes:

```java
// Service Two routes
router.get("/api/service-two/:id").handler(handlers.get("getServiceTwoItem")::handle);
router.post("/api/service-two").handler(handlers.get("createServiceTwoItem")::handle);
router.get("/api/service-two/random").handler(handlers.get("getServiceTwoRandomWeather")::handle);
router.get("/api/service-two/forecast/:city").handler(handlers.get("getServiceTwoForecast")::handle);
router.get("/api/service-two/cities").handler(handlers.get("getServiceTwoCities")::handle);
router.get("/api/service-two/stats").handler(handlers.get("getServiceTwoStats")::handle);
```

## 3. Update ServiceTwoHandler.java (Optional Enhancement)

While not strictly necessary, you could enhance the ServiceTwoHandler to explicitly handle the new endpoints:

```java
@Override
protected void handleRequest(RoutingContext context) {
    logger.debug("Handling Service Two request: {}", context.request().uri());
    
    JsonObject request = createRequestObject(context);
    
    // Set action parameter based on the path
    String path = context.request().path();
    if (path.endsWith("/random")) {
        // No action needed, default behavior will return random weather
    } else if (path.contains("/forecast/")) {
        request.put("action", "forecast");
    } else if (path.endsWith("/cities")) {
        request.put("action", "cities");
    } else if (path.endsWith("/stats")) {
        request.put("action", "stats");
    }
    
    // Call the parent handler with the enhanced request
    super.handleRequest(context, request);
}
```

## Implementation Notes

1. **Route Order Matters**: The order of route registration is important. More specific routes (like `/api/service-two/random`) should be registered before more general routes (like `/api/service-two/:id`). Otherwise, a request to `/api/service-two/random` would be handled by the `getServiceTwoItem` handler with `id=random`.

2. **Parameter Handling**: The WeatherHandler expects specific parameters:
   - For forecast: an "action" parameter set to "forecast" and a "city" parameter
   - For cities: an "action" parameter set to "cities"
   - For stats: an "action" parameter set to "stats"
   - For random weather: no specific parameters

3. **Testing**: After implementing these changes, test each endpoint to ensure it correctly triggers the corresponding operation in the WeatherHandler.

By implementing these changes, all the specialized operations in service-two's WeatherHandler will be properly exposed through the public API, making them accessible to clients.