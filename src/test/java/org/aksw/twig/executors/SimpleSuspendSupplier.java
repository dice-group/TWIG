package org.aksw.twig.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Callable;

class SimpleSuspendSupplier implements SuspendSupplier<Integer> {

    private static final Logger LOGGER = LogManager.getLogger(SimpleSuspendSupplier.class);

    static final int EXPECTED_RESULT = 100;

    private static final int STEPS = 100;

    private int doneIndex = 0;

    private int result = 0;

    int getResult() {
        return result;
    }

    @Override
    synchronized public Callable<Integer> next() {
        if (doneIndex++ >= STEPS) {
            return null;
        }

        return () -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }

            return doneIndex;
        };
    }

    @Override
    public void addResult(Integer result) {
        this.result++;
    }
}
