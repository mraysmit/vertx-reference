package dev.mars.vertx.common.util;

import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for handling exceptions in asynchronous code.
 * Provides methods for safe execution of code that might throw exceptions.
 */
public class ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    
    /**
     * Handles an exception by logging it and returning a failed Future.
     * 
     * @param <T> the result type
     * @param e the exception to handle
     * @param errorMessage the error message to log
     * @return a failed Future with the exception
     */
    public static <T> Future<T> handleException(Throwable e, String errorMessage) {
        logger.error(errorMessage, e);
        return Future.failedFuture(e);
    }
}