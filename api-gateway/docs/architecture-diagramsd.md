# Architecture Diagrams for Vert.x Reference Project

This document contains various diagrams illustrating the architecture and flow of the Vert.x Reference Project.

## System Architecture

```mermaid
graph TD
    Client[Client] -->|HTTP Request| AG[API Gateway]
    AG -->|Event Bus| S1[Service One]
    AG -->|Event Bus| S2[Service Two]
    S1 -->|Database Access| DB1[(Database 1)]
    S2 -->|Database Access| DB2[(Database 2)]
    
    subgraph "API Gateway Module"
        AG
    end
    
    subgraph "Microservices"
        S1
        S2
    end
    
    subgraph "Data Layer"
        DB1
        DB2
    end
```

## Request Flow Sequence

```mermaid
sequenceDiagram
    participant Client
    participant ApiGateway
    participant RouterFactory
    participant ServiceHandler
    participant MicroserviceClient
    participant CircuitBreaker
    participant EventBus
    participant Microservice
    
    Client->>ApiGateway: HTTP Request
    ApiGateway->>RouterFactory: Route Request
    RouterFactory->>ServiceHandler: Handle Request
    ServiceHandler->>MicroserviceClient: Send Request
    MicroserviceClient->>CircuitBreaker: Execute with Protection
    CircuitBreaker->>EventBus: Send via Event Bus
    EventBus->>Microservice: Process Request
    Microservice-->>EventBus: Response
    EventBus-->>CircuitBreaker: Return Response
    CircuitBreaker-->>MicroserviceClient: Return Response
    MicroserviceClient-->>ServiceHandler: Return Response
    ServiceHandler-->>RouterFactory: Return Response
    RouterFactory-->>ApiGateway: Return Response
    ApiGateway-->>Client: HTTP Response
```

## Component Class Diagram

```mermaid
classDiagram
    class ApiGatewayVerticle {
        -HttpServer server
        -MicroserviceClientFactory clientFactory
        -RouterFactory routerFactory
        +start(Promise~Void~ startPromise)
        +stop(Promise~Void~ stopPromise)
    }
    
    class RouterFactory {
        -Vertx vertx
        -JsonObject config
        +createRouter(RequestHandler healthCheckHandler, RequestHandler serviceOneHandler, RequestHandler serviceTwoHandler)
        -configureCors(Router router)
        -configureFallbackHandler(Router router)
        -configureErrorHandler(Router router)
    }
    
    class RequestHandler {
        <<interface>>
        +handle(RoutingContext context)
    }
    
    class AbstractRequestHandler {
        #Logger logger
        +handle(RoutingContext context)
        #handleRequest(RoutingContext context)*
        #handleError(RoutingContext context, Throwable e)
        #sendError(RoutingContext context, int statusCode, String message)
        #sendResponse(RoutingContext context, JsonObject response)
    }
    
    class ServiceRequestHandler {
        -MicroserviceClient serviceClient
        -String serviceName
        #handleRequest(RoutingContext context)
        #createRequestObject(RoutingContext context)
    }
    
    class ServiceOneHandler {
        #handleRequest(RoutingContext context)
    }
    
    class ServiceTwoHandler {
        #handleRequest(RoutingContext context)
    }
    
    class HealthCheckHandler {
        #handleRequest(RoutingContext context)
    }
    
    class MicroserviceClientFactory {
        -Vertx vertx
        -JsonObject config
        -Map~String, MicroserviceClient~ clients
        +getClient(String serviceName)
        -createClient(String serviceName)
        -createCircuitBreaker(String serviceName, JsonObject serviceConfig)
    }
    
    class MicroserviceClient {
        -EventBusService eventBusService
        -CircuitBreaker circuitBreaker
        -String serviceAddress
        +sendRequest(JsonObject request)
    }
    
    ApiGatewayVerticle --> RouterFactory
    ApiGatewayVerticle --> MicroserviceClientFactory
    RequestHandler <|.. AbstractRequestHandler
    AbstractRequestHandler <|-- ServiceRequestHandler
    ServiceRequestHandler <|-- ServiceOneHandler
    ServiceRequestHandler <|-- ServiceTwoHandler
    AbstractRequestHandler <|-- HealthCheckHandler
    MicroserviceClientFactory --> MicroserviceClient
    ServiceRequestHandler --> MicroserviceClient
```

## Circuit Breaker State Diagram

```mermaid
stateDiagram-v2
    [*] --> Closed
    Closed --> Open: Failure Threshold Reached
    Open --> HalfOpen: Timeout Period Elapsed
    HalfOpen --> Closed: Successful Request
    HalfOpen --> Open: Failed Request
    Open --> [*]
```

## ASCII Art Diagram (Alternative)

If Mermaid diagrams cannot be rendered, here's a simple ASCII art representation of the system architecture:

```
+----------------+      +----------------+      +----------------+
|                |      |                |      |                |
|     Client     +----->+  API Gateway   +----->+  Microservice  |
|                |      |                |      |                |
+----------------+      +-------+--------+      +----------------+
                               |
                               |
                               v
                        +------+-------+
                        |              |
                        |   Database   |
                        |              |
                        +--------------+
```

## How to View These Diagrams

1. **Mermaid Diagrams**:
    - GitHub automatically renders Mermaid diagrams in markdown files
    - Use the [Mermaid Live Editor](https://mermaid.live/) to view and edit these diagrams
    - Many IDEs have Mermaid plugins (VS Code, IntelliJ, etc.)

2. **ASCII Art**:
    - Viewable in any text editor or markdown viewer
    - No special tools required

## Additional Diagram Types Available

- Entity-Relationship Diagrams (ERD)
- User Flow Diagrams
- Network Topology Diagrams
- Deployment Diagrams
- Gantt Charts for Project Planning