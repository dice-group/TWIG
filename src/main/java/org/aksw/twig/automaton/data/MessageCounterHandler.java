package org.aksw.twig.automaton.data;

import org.aksw.twig.executors.FileReadingSuspendSupplier;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Creates multiple {@link MessageCounter} objects by parsing files as {@link TWIGModelWrapper} and
 * adding them to a {@link MessageCounter}. Parsed objects will then be merged into a result.
 */
public class MessageCounterHandler extends FileReadingSuspendSupplier<MessageCounter> {

  private static final Logger LOGGER = LogManager.getLogger(MessageCounterHandler.class);

  private final MessageCounter mergedResult = new MessageCounter();

  /**
   * Creates a new instance setting class variables.
   * 
   * @param filesToParse Set of files to parse.
   */
  public MessageCounterHandler(Set<File> filesToParse) {
    super(filesToParse);
  }

  @Override
  public Callable<MessageCounter> getFileProcessor(File file) {
    return () -> {
      LOGGER.info("Parsing file {}", file.getName());
      MessageCounter counter = new MessageCounter();
      counter.addModel(TWIGModelWrapper.read(file).getModel());
      return counter;
    };
  }

  @Override
  public void addResult(MessageCounter result) {
    synchronized (mergedResult) {
      LOGGER.info("Merging result");
      mergedResult.merge(result);
    }
  }

  @Override
  public MessageCounter getMergedResult() {
    return mergedResult;
  }

  /**
   * Runs a {@link org.aksw.twig.executors.SelfSuspendingExecutor} with a
   * {@link MessageCounterHandler} as {@link org.aksw.twig.executors.SuspendSupplier}. Arguments
   * must state a file to serialize the merged {@link MessageCounter}. Arguments must list files to
   * parse and must be formatted as stated in {@link FileHandler#readArgs(String[])}.
   * 
   * @param args Arguments.
   */
  public static void main(String[] args) {
    Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
    MessageCounterHandler handler = new MessageCounterHandler(fileArgs.getRight());
    FileReadingSuspendSupplier.start("message_count.obj", fileArgs.getLeft(), handler);
  }
}
