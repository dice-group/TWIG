package org.aksw.twig.executors;

import java.util.concurrent.Callable;

/**
 * Argument to {@link SelfSuspendingExecutor}. It supplies {@link Callable} objects to the executor and consumes results of type {@link T} from the executor.
 * @param <T> Result type to consume.
 */
public interface SuspendSupplier<T> {

    /**
     * Supplies a {@link Callable}. Method should be safe for concurrent access.
     * @return Callable to supply.
     */
    Callable<T> next();

    /**
     * Consumes a result.
     * @param result Result to consume. Method should be safe for concurrent access.
     */
    void addResult(T result);
}
