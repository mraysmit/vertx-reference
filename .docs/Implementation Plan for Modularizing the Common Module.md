# Implementation Plan for Modularizing the Common Module

Based on the analysis of the design.md document and the current project structure, I recommend the following implementation plan for splitting the common module into separate modules.

## 1. Project Structure Changes

Create the following new Maven modules:

1. **common-core**: Basic utilities and shared code
2. **common-config**: Configuration management utilities
3. **common-eventbus**: Event bus utilities
4. **common-metrics**: Metrics collection and reporting
5. **common-resilience**: Circuit breaker and resilience patterns

## 2. Implementation Steps

### Phase 1: Setup New Module Structure

1. **Create new module directories and POM files**:
   - Create directories for each new module
   - Create a pom.xml for each module with appropriate parent-child relationships
   - Update the parent pom.xml to include the new modules

2. **Configure dependencies for each module**:
   - **common-core**: 
     - io.vertx:vertx-core
     - SLF4J and Logback
   - **common-config**: 
     - common-core (new dependency)
     - io.vertx:vertx-config
   - **common-eventbus**: 
     - common-core (new dependency)
     - io.vertx:vertx-core
   - **common-metrics**: 
     - common-core (new dependency)
     - io.vertx:vertx-micrometer-metrics
     - io.micrometer:micrometer-registry-prometheus
   - **common-resilience**: 
     - common-core (new dependency)
     - io.vertx:vertx-circuit-breaker

### Phase 2: Code Migration

1. **Move utility classes to common-core**:
   - Move ExceptionHandler, ShutdownManager, and ThreadPoolConfig from common/util to common-core/util

2. **Move package-specific code to respective modules**:
   - Move ConfigLoader from common/config to common-config
   - Move EventBusService from common/eventbus to common-eventbus
   - Move MetricsManager from common/metrics to common-metrics
   - Move CircuitBreakerFactory from common/resilience to common-resilience

3. **Update imports in all modules**:
   - Update import statements in all classes to reflect the new package structure
   - For example, change `import dev.mars.vertx.common.config.ConfigLoader` to `import dev.mars.vertx.common.config.ConfigLoader`

### Phase 3: Update Dependent Modules

1. **Update api-gateway, service-one, and service-two dependencies**:
   - Replace the dependency on the common module with dependencies on the specific modules they need
   - For example, if api-gateway uses metrics and resilience, add dependencies on common-metrics and common-resilience

2. **Update import statements in dependent modules**:
   - Update import statements in all classes to reflect the new package structure

### Phase 4: Testing and Validation

1. **Run unit tests for each module**:
   - Ensure all tests pass after the refactoring
   - Add new tests if necessary to validate the modular structure

2. **Integration testing**:
   - Run the application to ensure all components work together correctly
   - Test different deployment scenarios (e.g., with and without metrics)

## 3. Dependency Management Strategy

1. **Use Maven BOM (Bill of Materials)**:
   - Create a common-bom module that defines versions for all common modules
   - Dependent projects can import this BOM to ensure consistent versioning

2. **Version alignment**:
   - Initially, all common modules should have the same version
   - As they evolve independently, they can have different versions

## 4. Migration Timeline

1. **Phase 1 (Setup)**: 1-2 days
2. **Phase 2 (Code Migration)**: 2-3 days
3. **Phase 3 (Update Dependencies)**: 1-2 days
4. **Phase 4 (Testing)**: 2-3 days

Total estimated time: 6-10 days, depending on project complexity and team size.

## 5. Benefits of This Approach

1. **Gradual Migration**: The step-by-step approach minimizes disruption to the development process
2. **Clear Dependencies**: Each module has clear and minimal dependencies
3. **Flexible Deployment**: Services can include only the modules they need
4. **Independent Evolution**: Each module can evolve at its own pace
5. **Improved Build Times**: Smaller modules lead to faster build times

## 6. Potential Challenges and Mitigations

1. **Challenge**: Circular dependencies between modules
   **Mitigation**: Carefully design module boundaries and extract shared code to common-core

2. **Challenge**: Increased complexity in dependency management
   **Mitigation**: Use Maven BOM and clear versioning strategy

3. **Challenge**: Potential duplication of code
   **Mitigation**: Regular code reviews and refactoring to identify and eliminate duplication

## 7. Future Considerations

1. **Integration Modules**: If needed, create integration modules that combine functionality from multiple modules
2. **Deployment Artifacts**: Consider creating pre-packaged artifacts that bundle commonly used modules
3. **Documentation**: Maintain clear documentation about which modules are needed for which functionality

This implementation plan provides a structured approach to modularizing the common module while minimizing disruption to the development process and maximizing the benefits of modularization.