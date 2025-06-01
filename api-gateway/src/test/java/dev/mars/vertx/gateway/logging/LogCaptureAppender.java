package dev.mars.vertx.gateway.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A log appender that captures log events for testing.
 * This class can be used to verify that specific log messages are generated during tests.
 */
public class LogCaptureAppender extends AppenderBase<ILoggingEvent> {
    
    private static final LogCaptureAppender INSTANCE = new LogCaptureAppender();
    private final List<ILoggingEvent> events = new ArrayList<>();
    
    private LogCaptureAppender() {
        setName("LogCaptureAppender");
    }
    
    /**
     * Gets the singleton instance of the appender.
     *
     * @return the appender instance
     */
    public static LogCaptureAppender getInstance() {
        return INSTANCE;
    }
    
    /**
     * Attaches the appender to the specified logger.
     *
     * @param loggerName the name of the logger to attach to
     */
    public void attachToLogger(String loggerName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(loggerName);
        
        // Detach first in case it's already attached
        logger.detachAppender(getName());
        
        // Attach and start
        logger.addAppender(this);
        start();
    }
    
    /**
     * Detaches the appender from the specified logger.
     *
     * @param loggerName the name of the logger to detach from
     */
    public void detachFromLogger(String loggerName) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger logger = loggerContext.getLogger(loggerName);
        
        logger.detachAppender(getName());
    }
    
    /**
     * Clears all captured log events.
     */
    public void clearEvents() {
        events.clear();
    }
    
    /**
     * Gets all captured log events.
     *
     * @return the list of log events
     */
    public List<ILoggingEvent> getEvents() {
        return new ArrayList<>(events);
    }
    
    /**
     * Gets all captured log events with the specified level.
     *
     * @param level the log level
     * @return the list of log events with the specified level
     */
    public List<ILoggingEvent> getEventsForLevel(Level level) {
        return events.stream()
                .filter(event -> event.getLevel().equals(level))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets all captured log events containing the specified message.
     *
     * @param message the message to search for
     * @return the list of log events containing the specified message
     */
    public List<ILoggingEvent> getEventsContainingMessage(String message) {
        return events.stream()
                .filter(event -> event.getFormattedMessage().contains(message))
                .collect(Collectors.toList());
    }
    
    /**
     * Checks if any captured log event contains the specified message.
     *
     * @param message the message to search for
     * @return true if any log event contains the specified message, false otherwise
     */
    public boolean hasEventContainingMessage(String message) {
        return events.stream()
                .anyMatch(event -> event.getFormattedMessage().contains(message));
    }
    
    /**
     * Checks if any captured log event with the specified level contains the specified message.
     *
     * @param level the log level
     * @param message the message to search for
     * @return true if any log event with the specified level contains the specified message, false otherwise
     */
    public boolean hasEventWithLevelContainingMessage(Level level, String message) {
        return events.stream()
                .filter(event -> event.getLevel().equals(level))
                .anyMatch(event -> event.getFormattedMessage().contains(message));
    }
    
    @Override
    protected void append(ILoggingEvent event) {
        events.add(event);
    }
}