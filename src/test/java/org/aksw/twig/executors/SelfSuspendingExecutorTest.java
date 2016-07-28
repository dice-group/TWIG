package org.aksw.twig.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class SelfSuspendingExecutorTest {

    private static final Logger LOGGER = LogManager.getLogger(SelfSuspendingExecutorTest.class);

    private boolean asserted = false;


    @Test
    public void simpleTest() {
        for (int i = 0; i < 10; i++) {
            execSimpleTest();
        }
    }

    private void execSimpleTest() {
        SimpleSuspendSupplier supplier = new SimpleSuspendSupplier();
        SelfSuspendingExecutor<Integer> executor = new SelfSuspendingExecutor<>(supplier);
        executor.addFinishedListener(() -> {
            asserted = true;
            Assert.assertEquals(SimpleSuspendSupplier.EXPECTED_RESULT, supplier.getResult());
        });
        executor.start();

        blockUntilFinished(executor);

        Assert.assertTrue(asserted);
    }

    @Test
    public void emptyTest() {
        for (int i = 0; i < 10; i++) {
            execEmptyTest();
        }
    }

    private void execEmptyTest() {
        SuspendSupplier<Integer> supplier = new EmptySuspendSupplier();
        SelfSuspendingExecutor<Integer> executor = new SelfSuspendingExecutor<>(supplier);
        executor.start();

        blockUntilFinished(executor);
    }

    @Test
    public void onceSupplyingTest() {
        for (int i = 0; i < 10; i++) {
            execOnceSupplyingTest();
        }
    }

    private void execOnceSupplyingTest() {
        OnceSuspendSupplier supplier = new OnceSuspendSupplier();
        SelfSuspendingExecutor<Integer> executor = new SelfSuspendingExecutor<>(supplier);
        executor.start();

        blockUntilFinished(executor);

        Assert.assertEquals(1, supplier.getResult());
    }

    private static void blockUntilFinished(SelfSuspendingExecutor executor) {
        while (!executor.isFinished()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }
}
