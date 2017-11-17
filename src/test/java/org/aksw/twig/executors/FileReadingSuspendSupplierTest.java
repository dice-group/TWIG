package org.aksw.twig.executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileReadingSuspendSupplierTest {

  private static final Logger LOGGER = LogManager.getLogger(FileReadingSuspendSupplierTest.class);

  private final BooleanWrapper assertedHolder = new BooleanWrapper();

  @Test
  public void test() {
    ClassLoader classLoader = FileReadingSuspendSupplierTest.class.getClassLoader();
    File file0, file1;
    try {
      file0 = new File(classLoader.getResource("testing/file_0").getPath());
      file1 = new File(classLoader.getResource("testing/file_1").getPath());
    } catch (NullPointerException e) {
      LOGGER.error(e.getMessage(), e);
      return;
    }
    List<File> files = Stream.of(file0, file1).collect(Collectors.toList());

    SimpleFileReadingSuspendSupplier suspendSupplier = new SimpleFileReadingSuspendSupplier(files);
    SelfSuspendingExecutor<Integer> executor = new SelfSuspendingExecutor<>(suspendSupplier);
    executor.addFinishedEventListeners(() -> {
      Assert.assertEquals(new Integer(2), suspendSupplier.getMergedResult());
      synchronized (assertedHolder) {
        assertedHolder.asserted = true;
      }
    });
    executor.start();

    while (true) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        LOGGER.error(e.getMessage(), e);
      }

      synchronized (assertedHolder) {
        if (assertedHolder.asserted) {
          return;
        }
      }
    }
  }

  private class SimpleFileReadingSuspendSupplier extends FileReadingSuspendSupplier<Integer> {

    int mergedResult;

    SimpleFileReadingSuspendSupplier(Collection<File> filesToParse) {
      super(filesToParse);
    }

    @Override
    public synchronized void addResult(Integer result) {
      mergedResult++;
    }

    @Override
    protected Callable<Integer> getFileProcessor(File file) {
      return () -> 0;
    }

    @Override
    protected Integer getMergedResult() {
      return mergedResult;
    }
  }

  private class BooleanWrapper {

    boolean asserted = false;
  }
}
