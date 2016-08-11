package org.aksw.twig.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class works like a {@link Executors#newFixedThreadPool(int)} with one difference. {@link Callable} objects get pulled from a {@link SuspendSupplier} which will then be executed.
 * Results will be handed vice versa to the {@link SuspendSupplier}. The major advantage of this class is that it will only pull and execute {@link Callable} objects from its supplier as long as there are available threads.
 * When there are no free threads available the executor will wait until there are. This is especially useful if queued tasks on normal executors consume a lot of RAM or task creation is much more faster than task processing.
 * @param <T> Type of the results provided by the {@link Callable} objects.
 */
public class SelfSuspendingExecutor<T> {

    private static final Logger LOGGER = LogManager.getLogger(SelfSuspendingExecutor.class);

    private int nThreads = 32;

    private final SuspendSupplier<T> suspendSupplier;

    /**
     * Executes by {@link #suspendSupplier} supplied callables.
     */
    private final ExecutorService executorService;

    /**
     * Executes code querying the {@link #suspendSupplier}.
     */
    private final ExecutorService callableSubmittingService;

    private final Queue<Runnable> finishedEventListeners = new LinkedList<>();

    private boolean finished = false;

    /**
     * Creates a new instance setting class variables.
     * @param suspendSupplier Supplier for new callables.
     */
    public SelfSuspendingExecutor(SuspendSupplier<T> suspendSupplier) {
        this.suspendSupplier = suspendSupplier;
        this.executorService = Executors.newFixedThreadPool(nThreads);
        this.callableSubmittingService = Executors.newSingleThreadExecutor();
    }

    /**
     * Creates a new instance setting class variables.
     * @param suspendSupplier Supplier for new callables.
     * @param nThreads Number of threads to work on callables.
     */
    public SelfSuspendingExecutor(SuspendSupplier<T> suspendSupplier, int nThreads) {
        this(suspendSupplier);
        this.nThreads = nThreads;
    }

    /**
     * Adds finished listeners to the executor. Listeners will be executed once all callables have been executed. If every callable has already been executed listeners will be run immediately.
     * @param listeners Listeners to execute.
     */
    public void addFinishedEventListeners(Runnable... listeners) {
        synchronized (finishedEventListeners) {
            if (finished) {
                for (Runnable r: listeners) {
                    r.run();
                }
            } else {
                Collections.addAll(finishedEventListeners, listeners);
            }
        }
    }

    /**
     * Starts callable processing. Method is non-blocking, i. e. termination of this method does not mean every callable has been executed.
     */
    public void start() {
        callableSubmittingService.execute(() -> {
            for (int i = 0; i < nThreads; i++) {
                execNext();
            }
        });
    }

    /**
     * Executes the next callable supplied by {@link #suspendSupplier} or finishes execution.
     */
    private void execNext() {
        LOGGER.info("Querying callable");
        Callable<T> callable = suspendSupplier.next();
        if (callable == null) {
            if (!callableSubmittingService.isShutdown()) {
                callableSubmittingService.execute(this::finish);
            }
            return;
        }
        executorService.execute(() -> runCallable(callable));
    }

    /**
     * Finishes the execution by shutting down services, etc.
     */
    private void finish() {
        if (executorService.isShutdown()) {
            return;
        }

        LOGGER.info("Finishing");

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        synchronized (finishedEventListeners) {
            finishedEventListeners.forEach(Runnable::run);
            finishedEventListeners.clear();
            finished = true;
            callableSubmittingService.shutdown();
        }
    }

    /**
     * Returns whether every callable has been executed.
     * @return True iff every callable has been executed.
     */
    public boolean isFinished() {
        synchronized (finishedEventListeners) {
            return finished;
        }
    }

    /**
     * Executes a callable.
     * @param callable Callable to execute.
     */
    private void runCallable(Callable<T> callable) {
        try {
            LOGGER.info("Executing callable");
            T result = callable.call();
            suspendSupplier.addResult(result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        callableSubmittingService.execute(this::execNext);
    }
}
