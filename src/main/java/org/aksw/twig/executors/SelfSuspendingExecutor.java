package org.aksw.twig.executors;

import com.google.common.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * This class works like a {@link Executors#newFixedThreadPool(int)} with one difference. {@link Callable} objects get pulled from a {@link SuspendSupplier} which will then be executed.
 * Results will be handed vice versa to the {@link SuspendSupplier}. The major advantage of this class is that it will only pull and execute {@link Callable} objects from its supplier as long as there are available threads.
 * When there are no free threads available the executor will wait until there are. This is especially useful if queued tasks on normal executors consume a lot of RAM or task creation is much more faster than task processing.
 * @param <T> Type of the results provided by the {@link Callable} objects.
 */
public class SelfSuspendingExecutor<T> {

    private static final Logger LOGGER = LogManager.getLogger(SelfSuspendingExecutor.class);

    private static final int N_THREADS_DEFAULT = 32;

    private final int nThreads;

    /**
     * Executes by {@link #suspendSupplier} supplied callables.
     */
    private final ListeningExecutorService executorService;

    private final SuspendSupplier<T> suspendSupplier;

    /**
     * Executes listening to callbacks.
     */
    private final ExecutorCallback callback = new ExecutorCallback();

    /**
     * Threads callback listening.
     */
    private final Executor singleThreadedCallbackExecutor = Executors.newSingleThreadExecutor();

    /**
     * Threads callable supplying.
     */
    private final Executor singleThreadedSelfExecutor = Executors.newSingleThreadExecutor();

    private final List<Runnable> finishedListeners = new LinkedList<>();

    private boolean started = false;

    /**
     * Creates a new instance setting class variables.
     * @param suspendSupplier Supplier for new callables.
     */
    public SelfSuspendingExecutor(SuspendSupplier<T> suspendSupplier) {
        this(suspendSupplier, N_THREADS_DEFAULT);
    }

    /**
     * Creates a new instance setting class variables.
     * @param suspendSupplier Supplier for new callables.
     * @param nThreads Number of threads to work on callables.
     */
    public SelfSuspendingExecutor(SuspendSupplier<T> suspendSupplier, int nThreads) {
        this.nThreads = nThreads;
        this.suspendSupplier = suspendSupplier;
        this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(nThreads));
    }

    /**
     * Adds finished listeners to the executor. Listeners will be executed once all callables have been executed. If every callable has already been executed listeners will be run immediately.
     * @param listeners Listeners to execute.
     */
    public void addFinishedListener(Runnable... listeners) {
        synchronized (finishedListeners) {
            if (executorService.isTerminated()) {
                for (Runnable listener : listeners) {
                    listener.run();
                }
            } else {
                Collections.addAll(finishedListeners, listeners);
            }
        }
    }

    /**
     * Returns whether every callable has been executed.
     * @return True iff every callable has been executed.
     */
    public boolean isFinished() {
        return executorService.isTerminated() && finishedListeners.isEmpty();
    }

    /**
     * Starts to work on callables. Method is non-blocking, i. e. termination of this method does not mean every callable has been executed.
     */
    public void start() {
        if (started) {
            throw new IllegalStateException("Executor already started");
        }
        started = true;

        for (int i = 0; i < nThreads; i++) {
            singleThreadedSelfExecutor.execute(this::execNext);
        }
    }

    /**
     * Finishes the execution by shutting down services, etc.
     */
    private void finish() {
        if (executorService.isShutdown()) {
            throw new IllegalStateException();
        }

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            try { Thread.sleep(100); } catch (InterruptedException e) { LOGGER.error(e.getMessage(), e); }
        }

        synchronized (finishedListeners) {
            finishedListeners.forEach(runnable -> {
                try {
                    runnable.run();
                } catch (RuntimeException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            });
            finishedListeners.clear();
        }
    }

    /**
     * Executes the next callable supplied by {@link #suspendSupplier} or finishes execution.
     */
    private void execNext() {
        if (executorService.isShutdown()) {
            return;
        }

        Callable<T> task = suspendSupplier.next();

        if (task == null) {
            finish();
            return;
        }

        ListenableFuture<T> listenableFuture = executorService.submit(task);
        Futures.addCallback(listenableFuture, callback, singleThreadedCallbackExecutor);
        listenableFuture.addListener(this::execNext, singleThreadedSelfExecutor);
    }

    /**
     * Class to handle future listening.
     */
    private class ExecutorCallback implements FutureCallback<T> {

        @Override
        public void onSuccess(T result) {
            suspendSupplier.addResult(result);
        }

        @Override
        public void onFailure(Throwable t) {
            LOGGER.error(t.getMessage(), t);
        }
    }
}
