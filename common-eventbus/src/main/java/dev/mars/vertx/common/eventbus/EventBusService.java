package dev.mars.vertx.common.eventbus;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Utility class for working with the Vert.x Event Bus.
 * Provides methods for sending messages, registering consumers, and handling responses.
 * This implementation includes enhanced logging for better diagnostics.
 */
public class EventBusService {
    private static final Logger logger = LoggerFactory.getLogger(EventBusService.class);
    private static final long DEFAULT_TIMEOUT = 30000; // 30 seconds
    
    private final EventBus eventBus;
    
    /**
     * Creates a new EventBusService.
     *
     * @param vertx the Vertx instance
     */
    public EventBusService(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        logger.info("EventBusService initialized");
    }
    
    /**
     * Sends a message to the event bus and expects a reply.
     *
     * @param <T> the type of the reply
     * @param address the address to send the message to
     * @param message the message to send
     * @param replyClass the class of the expected reply
     * @return a Future with the reply
     */
    public <T> Future<T> send(String address, Object message, Class<T> replyClass) {
        return send(address, message, replyClass, DEFAULT_TIMEOUT);
    }
    
    /**
     * Sends a message to the event bus and expects a reply with a custom timeout.
     *
     * @param <T> the type of the reply
     * @param address the address to send the message to
     * @param message the message to send
     * @param replyClass the class of the expected reply
     * @param timeoutMs the timeout in milliseconds
     * @return a Future with the reply
     */
    public <T> Future<T> send(String address, Object message, Class<T> replyClass, long timeoutMs) {
        logger.debug("Sending message to {}: {}", address, message);
        
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(timeoutMs);
        
        return Future.<Message<Object>>future(promise -> 
                eventBus.request(address, message, options, promise)
        ).map(reply -> {
            logger.debug("Received reply from {}: {}", address, reply.body());
            if (replyClass.isInstance(reply.body())) {
                return replyClass.cast(reply.body());
            } else if (reply.body() instanceof JsonObject && replyClass == JsonObject.class) {
                return replyClass.cast(reply.body());
            } else {
                throw new ClassCastException("Cannot cast reply to " + replyClass.getName());
            }
        }).onFailure(err -> 
            logger.error("Error sending message to {}: {}", address, err.getMessage())
        );
    }
    
    /**
     * Publishes a message to the event bus without expecting a reply.
     *
     * @param address the address to publish the message to
     * @param message the message to publish
     */
    public void publish(String address, Object message) {
        logger.debug("Publishing message to {}: {}", address, message);
        eventBus.publish(address, message);
    }
    
    /**
     * Registers a consumer for messages on the event bus.
     *
     * @param <T> the type of the message
     * @param address the address to listen on
     * @param handler the function to handle messages
     * @return the message consumer
     */
    public <T> MessageConsumer<T> consumer(String address, Function<T, Future<Object>> handler) {
        logger.info("Registering consumer for address: {}", address);
        
        return eventBus.<T>consumer(address, message -> {
            logger.debug("Received message on {}: {}", address, message.body());
            
            handler.apply(message.body())
                .onSuccess(reply -> {
                    logger.debug("Sending reply to {}: {}", address, reply);
                    message.reply(reply);
                })
                .onFailure(err -> {
                    logger.error("Error processing message on {}: {}", address, err.getMessage());
                    message.fail(500, err.getMessage());
                });
        });
    }
    
    /**
     * Unregisters a consumer from the event bus.
     *
     * @param consumer the consumer to unregister
     * @return a Future that completes when the consumer is unregistered
     */
    public Future<Void> unregisterConsumer(MessageConsumer<?> consumer) {
        logger.info("Unregistering consumer for address: {}", consumer.address());
        return consumer.unregister();
    }
    
    /**
     * Sets delivery options for a message.
     *
     * @param timeoutMs the timeout in milliseconds
     * @param headers optional headers to include
     * @return the delivery options
     */
    public DeliveryOptions createDeliveryOptions(long timeoutMs, JsonObject headers) {
        DeliveryOptions options = new DeliveryOptions().setSendTimeout(timeoutMs);
        
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(entry -> options.addHeader(entry.getKey(), entry.getValue().toString()));
            logger.debug("Added headers to delivery options: {}", headers.encode());
        }
        
        return options;
    }
}