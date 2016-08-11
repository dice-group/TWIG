package org.aksw.twig.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelfSuspendingExecutor<T> {

    private static final Logger LOGGER = LogManager.getLogger(SelfSuspendingExecutor.class);

    private int nThreads = 32;

    private final SuspendSupplier<T> suspendSupplier;

    private final ExecutorService executorService;

    private final ExecutorService callableSubmittingService;

    private final Queue<Runnable> finishedEventListeners = new LinkedList<>();

    private boolean finished = false;

    public SelfSuspendingExecutor(SuspendSupplier<T> suspendSupplier) {
        this.suspendSupplier = suspendSupplier;
        this.executorService = Executors.newFixedThreadPool(nThreads);
        this.callableSubmittingService = Executors.newSingleThreadExecutor();
    }

    public SelfSuspendingExecutor(SuspendSupplier<T> suspendSupplier, int nThreads) {
        this(suspendSupplier);
        this.nThreads = nThreads;
    }

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

    public void start() {
        callableSubmittingService.execute(() -> {
            for (int i = 0; i < nThreads; i++) {
                execNext();
            }
        });
    }

    private void execNext() {
        Callable<T> callable = suspendSupplier.next();
        if (callable == null) {
            if (!callableSubmittingService.isShutdown()) {
                callableSubmittingService.execute(this::finish);
            }
            return;
        }
        executorService.execute(() -> runCallable(callable));
    }

    private void finish() {
        if (executorService.isShutdown()) {
            return;
        }

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

    public boolean isFinished() {
        synchronized (finishedEventListeners) {
            return finished;
        }
    }

    private void runCallable(Callable<T> callable) {
        try {
            T result = callable.call();
            suspendSupplier.addResult(result);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        callableSubmittingService.execute(this::execNext);
    }
}
