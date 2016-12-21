package org.aksw.twig.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * This class reads a file that contains twitter7 data block-wise. Each block must be formatted by
 * following regex:<br/>
 * <ul>
 * <li>{T .*[\n]+ U .*[\n]+ W .*[\n]}+}*</li>
 * </ul>
 * Blocks that do not match this criteria will be skipped. Every matching block will be handed to a
 * {@link Callable} that gets specified in constructor. Every {@link FutureCallback} that has been
 * added by {@link #addFutureCallbacks(FutureCallback[])} will be added as listener to the
 * {@link Callable}.
 *
 * @param <T> Data type that will be returned by threaded parsers.
 *
 * @author Felix Linker
 */
public class Twitter7Parser<T> implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(Twitter7Parser.class);

  // TODO: parameter
  private static final int N_THREADS = 4;

  private final Function<Triple<String, String, String>, Callable<T>> resultParserSupplier;

  private final ListeningExecutorService service =
      MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(N_THREADS));

  private final Executor listenerExecutor = MoreExecutors.directExecutor();

  private final Runnable threadTerminatedListener = this::readTwitter7Block;

  private final List<FutureCallback<T>> futureCallbacks = new LinkedList<>();

  private final List<Runnable> parsingFinishedListeners = new LinkedList<>();

  private final BufferedReader fileReader;

  private boolean run = false;

  /**
   * Initializes a file reader to given file and sets class variables.
   * 
   * @param inputStream InputStream to read from.
   * @param resultParserSupplier Function to apply a triple - the twitter7 block reading result - to
   *        a callable parser.
   * @throws IOException Can be thrown by errors during reader creation.
   * @throws NullPointerException Thrown if any argument is {@code null}.
   */
  public Twitter7Parser(final InputStream inputStream,
      final Function<Triple<String, String, String>, Callable<T>> resultParserSupplier)
          throws IOException, NullPointerException {
    if ((resultParserSupplier == null) || (inputStream == null)) {
      throw new NullPointerException();
    }
    this.resultParserSupplier = resultParserSupplier;
    this.fileReader = new BufferedReader(new InputStreamReader(inputStream));
  }

  /**
   * Adds callbacks to the parser. Each {@link FutureCallback} will be added as listener to by the
   * {@code resultParserSupplier} results as explained in {@link Twitter7Parser}.
   * 
   * @param callbacks
   */
  public void addFutureCallbacks(final FutureCallback<T>... callbacks) {
    Collections.addAll(futureCallbacks, callbacks);
  }

  /**
   * Each listener will be called as soon if reading of the input stream has finished.
   * 
   * @param listeners Listener to call.
   */
  public void addParsingFinishedResultListeners(final Runnable... listeners) {
    Collections.addAll(parsingFinishedListeners, listeners);
  }

  /**
   * Starts reading of the given file. If you want to read the same file twice you cannot do this
   * with the same object.
   * 
   * @throws IllegalStateException Thrown if reader gets started twice.
   */
  @Override
  public void run() {
    if (run) {
      throw new IllegalStateException();
    }
    run = true;

    LOGGER.info("Started parsing file");

    // Start the initial threads.
    for (int i = 0; i < N_THREADS; i++) {
      readTwitter7Block();
    }

    LOGGER.info("Ended parsing file");
  }

  /**
   * Reads the next block of twitter7 data and hands it to the executor service.
   */
  private void readTwitter7Block() {
    if (service.isShutdown()) {
      return;
    }

    synchronized (fileReader) {

      recursion: while (true) {
        LOGGER.debug("mimics recursion iteratively");
        final MutableTriple<String, String, String> triple = new MutableTriple<>();
        READ_STATE readState = START_STATE;

        try {
          while (!readingFinished(readState)) {

            // Skip empty lines
            String line;
            while (((line = fileReader.readLine()) != null) && line.isEmpty()) {
              ;
            }
            if (line == null) {
              finish();
              return;
            }

            final String linePrefix = lineIdentifier(readState);
            if (line.startsWith(linePrefix)) {
              triplePutLine(readState, triple).accept(line.substring(linePrefix.length()));
              readState = nextState(readState);
            } else {

              LOGGER.error("Encountered malformed block in twitter7 data.");

              // Skip non-empty lines
              while (((line = fileReader.readLine()) != null) && !line.isEmpty()) {
                ;
              }
              if (line == null) {
                finish();
                return;
              }

              continue recursion; // "recursive" call
            }
          }
        } catch (final IOException e) {
          LOGGER.error(e.getMessage(), e);
          continue; // "recursive" call
        }

        final ListenableFuture<T> fut = service.submit(resultParserSupplier.apply(triple));
        futureCallbacks.forEach(callback -> Futures.addCallback(fut, callback));
        fut.addListener(threadTerminatedListener, listenerExecutor);
        return;
      }
    }
  }

  /**
   * All states during one invoke of {@link #readingFinished(READ_STATE)}.
   */
  private enum READ_STATE {
    READ_T, READ_U, READ_W, READ_FINISHED
  }

  /**
   * Start state of {@link #readingFinished(READ_STATE)}.
   */
  private static final READ_STATE START_STATE = READ_STATE.READ_T;

  /**
   * Returns whether the given state is the finishing one.
   * 
   * @param state State to check.
   * @return True if parsing can be finished.
   */
  private boolean readingFinished(final READ_STATE state) {
    switch (state) {
      case READ_FINISHED:
        return true;
    }

    return false;
  }

  /**
   * Returns the next state.
   * 
   * @param oldState Current state to get the next for.
   * @return Next state.
   * @throws IllegalStateException Thrown if {@code oldState} state has no next state.
   */
  private READ_STATE nextState(final READ_STATE oldState) throws IllegalStateException {
    switch (oldState) {
      case READ_T:
        return READ_STATE.READ_U;
      case READ_U:
        return READ_STATE.READ_W;
      case READ_W:
        return READ_STATE.READ_FINISHED;
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Twitter7 data has lines starting with different prefixes. This method returns the line start
   * for given state.
   * 
   * @param state State to get prefix for.
   * @return Prefix.
   * @throws IllegalStateException Thrown if {@code state} is finishing state.
   */
  private String lineIdentifier(final READ_STATE state) throws IllegalStateException {
    switch (state) {
      case READ_T:
        return "T";
      case READ_U:
        return "U";
      case READ_W:
        return "W";
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Returns consumer to a line. This consumer will set the line to the right triple element.
   * 
   * @param state State to get the consumer for.
   * @param triple Triple to store the consumed result in.
   * @return Consumer that stores a line in the triple.
   * @throws IllegalStateException Thrown if {@code state} is finishing state.
   */
  private Consumer<String> triplePutLine(final READ_STATE state,
      final MutableTriple<String, String, String> triple) throws IllegalStateException {
    switch (state) {
      case READ_T:
        return triple::setLeft;
      case READ_U:
        return triple::setMiddle;
      case READ_W:
        return triple::setRight;
      default:
        throw new IllegalStateException();
    }
  }

  /**
   * Finishes the reading by closing all streams and notifying listeners.
   */
  private void finish() {
    if (service.isShutdown()) {
      throw new IllegalStateException();
    }

    // Close file reader
    synchronized (fileReader) {
      try {
        fileReader.close();
      } catch (final IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    // Shutdown threaded execution service
    service.shutdown();
    while (!service.isTerminated()) {
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
        LOGGER.error(e.getMessage(), e);
      }
    }

    parsingFinishedListeners.forEach(Runnable::run);

    LOGGER.info("Finished parsing file");
  }

  /**
   * Parses one or more files according to twitter7 format. Arguments must be formatted as stated in
   * {@link FileHandler#readArgs(String[])} but {@code --out=} argument is mandatory. You should not
   * parse files with the same name from different directories as that could mess up the output.
   * 
   * @param args One or more arguments as specified above.
   * @see Twitter7Parser
   */
  public static void main(final String[] args) {

    final Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);
    final File outputDirectory = parsedArgs.getLeft();
    final Set<File> filesToParse = parsedArgs.getRight();

    // Start parsing
    final ExecutorService service = Executors.newFixedThreadPool(filesToParse.size());
    filesToParse.forEach(file -> {
      try {
        final InputStream inputStream = FileHandler.getDecompressionStreams(file);
        String fileName = file.getName();
        final int nameEndIndex = fileName.indexOf('.');
        fileName = fileName.substring(0, nameEndIndex == -1 ? fileName.length() : nameEndIndex);
        final Twitter7Parser<TWIGModelWrapper> parser =
            new Twitter7Parser<>(inputStream, Twitter7BlockParser::new);
        final Twitter7ResultCollector resultCollector =
            new Twitter7ResultCollector(fileName, outputDirectory);
        parser.addFutureCallbacks(resultCollector);
        parser.addParsingFinishedResultListeners(resultCollector::writeModel, () -> {
          try {
            inputStream.close();
          } catch (final IOException e) {
            LOGGER.error(e.getMessage(), e);
          }
        });
        service.execute(parser);
      } catch (final IOException e) {
        LOGGER.error(e.getMessage(), e);
      }
    });
    service.shutdown();
  }
}
