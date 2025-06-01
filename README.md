# Vert.x Reference Project

A multi-module reference project demonstrating best practices for building applications with Vert.x.

## Project Structure

The project is organized into the following modules:

- **common**: Shared code, utilities, and models used by other modules
- **api-gateway**: Entry point for the application, routes requests to microservices
- **service-one**: Sample microservice that manages items
- **service-two**: Sample microservice that provides weather data

## Features

This project demonstrates the following Vert.x best practices:

- **Modularity**: Using verticles to organize code by responsibility
- **Event Bus Communication**: Leveraging the event bus for inter-service communication
- **Circuit Breakers**: Implementing circuit breakers to handle failures gracefully
- **Metrics**: Using Micrometer with Prometheus for monitoring
- **Thread Pool Configuration**: Optimizing thread pool sizes for performance
- **Graceful Shutdown**: Properly cleaning up resources on shutdown
- **Exception Handling**: Properly handling exceptions in asynchronous code
- **Configuration Management**: Loading configuration from various sources
- **Testing**: Unit testing asynchronous code

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher

## Building the Project

To build the entire project:

```bash
mvn clean package
```

This will create executable JAR files for each module in their respective `target` directories.

## Running the Application

### Starting the API Gateway

```bash
java -jar api-gateway/target/api-gateway-1.0-SNAPSHOT-fat.jar
```

### Starting Service One

```bash
java -jar service-one/target/service-one-1.0-SNAPSHOT-fat.jar
```

### Starting Service Two

```bash
java -jar service-two/target/service-two-1.0-SNAPSHOT-fat.jar
```

## API Endpoints

The API Gateway exposes the following endpoints:

### Health Check

```
GET /health
```

Returns the health status of the API Gateway.

### Service One Endpoints

```
GET /api/service-one/:id
```

Gets an item by ID.

```
POST /api/service-one
```

Creates a new item. The request body should be a JSON object with the following fields:
- `action`: "create"
- `name`: The name of the item
- `description`: The description of the item

### Service Two Endpoints

```
GET /api/service-two?city=London
```

Gets weather data for a specific city.

```
POST /api/service-two
```

Gets weather data based on the request. The request body should be a JSON object with one of the following:
- `city`: The name of the city to get weather for
- `action`: One of "forecast", "cities", or "stats"

## Configuration

Each module has its own configuration file in `src/main/resources/config.yaml`. These files can be overridden by providing an external configuration file:

```bash
java -jar api-gateway/target/api-gateway-1.0-SNAPSHOT-fat.jar -conf path/to/config.yaml
```

## Metrics

Prometheus metrics are exposed on the following endpoints:

- API Gateway: http://localhost:9090/metrics
- Service One: http://localhost:9091/metrics
- Service Two: http://localhost:9092/metrics

## Testing

To run the tests:

```bash
mvn test
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
