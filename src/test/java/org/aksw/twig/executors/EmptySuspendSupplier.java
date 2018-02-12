package org.aksw.twig.executors;

import java.util.concurrent.Callable;

class EmptySuspendSupplier implements SuspendSupplier<Integer> {

  @Override
  public Callable<Integer> next() {
    return null;
  }

  @Override
  public void addResult(Integer result) {

  }
}
