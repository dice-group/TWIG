package org.aksw.twig.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class SelfSuspendingExecutorTest {

    private static final Logger LOGGER = LogManager.getLogger(SelfSuspendingExecutorTest.class);

    private boolean asserted = false;

    @Test
    public void test() {
        for (int i = 0; i < 100; i++) {
            execTest();
        }
    }

    public void execTest() {
        SimpleSuspendSupplier supplier = new SimpleSuspendSupplier();
        SelfSuspendingExecutor<Integer> executor = new SelfSuspendingExecutor<>(supplier);
        executor.addFinishedListener(() -> {
            asserted = true;
            Assert.assertEquals(SimpleSuspendSupplier.EXPECTED_RESULT, supplier.getResult());
        });
        executor.start();

        while (!executor.isFinished()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        Assert.assertTrue(asserted);
    }
}
