package org.aksw.twig.executors;

import java.util.concurrent.Callable;

class OnceSuspendSupplier implements SuspendSupplier<Integer> {

    private boolean supplied = false;

    private int result = 0;

    @Override
    public Callable<Integer> next() {
        if (supplied) {
            return null;
        }

        return () -> 1;
    }

    @Override
    public void addResult(Integer result) {
        this.result = result;
    }
}
