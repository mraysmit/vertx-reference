package dev.mars.vertx.gateway.service;

import dev.mars.vertx.common.eventbus.EventBusService;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for communicating with microservices via the event bus.
 * Encapsulates circuit breaker and event bus communication.
 */
public class MicroserviceClient {
    private static final Logger logger = LoggerFactory.getLogger(MicroserviceClient.class);
    
    private final EventBusService eventBusService;
    private final CircuitBreaker circuitBreaker;
    private final String serviceAddress;
    
    /**
     * Creates a new microservice client.
     *
     * @param vertx the Vertx instance
     * @param circuitBreaker the circuit breaker for this service
     * @param serviceAddress the event bus address of the service
     */
    public MicroserviceClient(Vertx vertx, CircuitBreaker circuitBreaker, String serviceAddress) {
        this.eventBusService = new EventBusService(vertx);
        this.circuitBreaker = circuitBreaker;
        this.serviceAddress = serviceAddress;
        
        logger.info("Created microservice client for address: {}", serviceAddress);
    }
    
    /**
     * Sends a request to the microservice with circuit breaker protection.
     *
     * @param request the request to send
     * @return a Future with the response
     */
    public Future<JsonObject> sendRequest(JsonObject request) {
        logger.debug("Sending request to service {}: {}", serviceAddress, request);
        
        return circuitBreaker.execute(promise -> 
            eventBusService.send(serviceAddress, request, JsonObject.class)
                .onSuccess(response -> {
                    logger.debug("Received response from service {}: {}", serviceAddress, response);
                    promise.complete(response);
                })
                .onFailure(err -> {
                    logger.error("Service {} request failed", serviceAddress, err);
                    promise.fail(err);
                })
        );
    }
}