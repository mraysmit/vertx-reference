# API Documentation

## OpenAPI Integration

The API Gateway includes OpenAPI integration, which provides interactive API documentation and a way to test the API endpoints. The OpenAPI specification is defined in the `openapi.yaml` file located in the `src/main/resources` directory.

## Accessing the API Documentation

The API documentation is available at the following endpoints:

- **Swagger UI**: `/swagger-ui` or `/docs` - Interactive API documentation
- **OpenAPI Specification**: `/openapi.yaml` - Raw OpenAPI specification file

## Available Endpoints

The API Gateway provides the following endpoints:

### Health Check

- **GET /health** - Returns the status of the API Gateway

### Service One

- **GET /api/service-one/{id}** - Get an item from Service One by ID
- **POST /api/service-one** - Create a new item in Service One

### Service Two

- **GET /api/service-two/{id}** - Get an item from Service Two by ID
- **POST /api/service-two** - Create a new item in Service Two

## Maintaining the OpenAPI Specification

The OpenAPI specification is defined in the `openapi.yaml` file. When adding new endpoints or modifying existing ones, make sure to update the specification file to reflect the changes.

### Key Components to Update

1. **Paths**: Define the endpoint paths, HTTP methods, and operations
2. **Schemas**: Define the request and response data models
3. **Tags**: Group related operations together
4. **Descriptions**: Provide clear descriptions for operations and parameters

### Example: Adding a New Endpoint

To add a new endpoint, add a new path to the `paths` section of the OpenAPI specification:

```yaml
/api/new-endpoint:
  get:
    summary: New endpoint description
    description: Detailed description of the new endpoint
    operationId: getNewEndpoint
    tags:
      - New Tag
    responses:
      '200':
        description: Successful response
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NewResponseSchema'
```

Then, add the corresponding schema to the `components/schemas` section:

```yaml
NewResponseSchema:
  type: object
  properties:
    id:
      type: string
      description: ID of the item
    name:
      type: string
      description: Name of the item
  required:
    - id
    - name
```

Finally, update the RouterFactory to map the new operation ID to the appropriate handler:

```java
handlers.put("getNewEndpoint", newEndpointHandler);
```

## Testing the API Documentation

The API Gateway includes tests to verify that the OpenAPI documentation is accessible and working correctly. These tests are located in the `ApiGatewayVerticleTest` class.

To run the tests, use the following command:

```bash
mvn test -Dtest=ApiGatewayVerticleTest
```