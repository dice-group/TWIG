package org.aksw.twig.executors;

import java.util.concurrent.Callable;

class OnceSuspendSupplier implements SuspendSupplier<Integer> {

    private boolean supplied = false;

    private int result = 0;

    public int getResult() {
        return result;
    }

    @Override
    public Callable<Integer> next() {
        if (supplied) {
            return null;
        }

        supplied = true;

        return () -> 1;
    }

    @Override
    public void addResult(Integer result) {
        this.result = result;
    }
}
