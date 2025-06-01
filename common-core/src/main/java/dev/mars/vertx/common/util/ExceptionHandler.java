package dev.mars.vertx.common.util;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Utility class for handling exceptions in asynchronous code.
 * Provides methods for safe execution of code that might throw exceptions.
 */
public class ExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    
    /**
     * Wraps a function that might throw an exception in a try-catch block and returns a Future.
     * 
     * @param <T> the input type
     * @param <R> the result type
     * @param function the function to execute
     * @param input the input to the function
     * @param errorMessage the error message to log if an exception occurs
     * @return a Future with the result or failure
     */
    public static <T, R> Future<R> wrapFunction(Function<T, R> function, T input, String errorMessage) {
        Promise<R> promise = Promise.promise();
        try {
            R result = function.apply(input);
            promise.complete(result);
        } catch (Exception e) {
            logger.error(errorMessage, e);
            promise.fail(e);
        }
        return promise.future();
    }
    
    /**
     * Wraps a runnable that might throw an exception in a try-catch block and returns a Future.
     * 
     * @param runnable the runnable to execute
     * @param errorMessage the error message to log if an exception occurs
     * @return a Future with void result or failure
     */
    public static Future<Void> wrapRunnable(Runnable runnable, String errorMessage) {
        Promise<Void> promise = Promise.promise();
        try {
            runnable.run();
            promise.complete();
        } catch (Exception e) {
            logger.error(errorMessage, e);
            promise.fail(e);
        }
        return promise.future();
    }
    
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
    
    /**
     * Logs an exception but doesn't propagate it.
     * Useful for fire-and-forget operations where you don't want to fail the caller.
     * 
     * @param e the exception to handle
     * @param errorMessage the error message to log
     */
    public static void logException(Throwable e, String errorMessage) {
        logger.error(errorMessage, e);
    }
}