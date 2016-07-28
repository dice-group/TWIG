package org.aksw.twig.automaton.time;

import org.aksw.twig.executors.SuspendSupplier;

import java.util.concurrent.Callable;

public class TimeCounterHandler implements SuspendSupplier<TimeCounter> {

    @Override
    public Callable<TimeCounter> next() {
        return null;
    }

    @Override
    public void addResult(TimeCounter result) {

    }
}
