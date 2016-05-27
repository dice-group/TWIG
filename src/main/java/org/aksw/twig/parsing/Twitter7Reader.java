package org.aksw.twig.parsing;

import com.google.common.util.concurrent.*;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class reads a file that contains twitter7 data block-wise.
 * Each block must be formatted by following regex:<br/>
 *      {T .*[\n]+<br/>
 *      U .*[\n]+<br/>
 *      W .*[\n]}+}*<br/>
 * Blocks that do not match this criteria will be skipped.
 *
 * @param <T> Type of the parsing result.
 * @author Felix Linker
 */
public class Twitter7Reader<T> implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Reader.class);

    private BufferedReader fileReader;

    private Supplier<FutureCallback<T>> callbackSupplier;

    private Function<Triple<String, String, String>, Callable<T>> resultParser;

    private Set<ListenableFuture<T>> unfinishedFutures;

    private boolean running = false;

    /**
     * Initializes a file reader to given file and sets class variables.
     * @param fileToParse File to parse.
     * @param callbackSupplier Supplier for result consumers.
     * @param resultParser Function to apply  a triple as twitter7 block reading result to a callable parser.
     * @throws IOException Can be thrown by errors during reader creation.
     * @throws NullPointerException Thrown if {@code callBackSupplier} or {@code resultParser} is {@code null}.
     */
    public Twitter7Reader(
            File fileToParse,
            Supplier<FutureCallback<T>> callbackSupplier,
            Function<Triple<String, String, String>, Callable<T>> resultParser) throws IOException, NullPointerException {
        if (callbackSupplier == null || resultParser == null) {
            throw new NullPointerException();
        }

        this.fileReader = new BufferedReader(new FileReader(fileToParse));
        this.callbackSupplier = callbackSupplier;
        this.resultParser = resultParser;
    }

    /**
     * Initializes a file reader to given file and sets class variables.
     * @param fileToParsePath File to parse.
     * @param callbackSupplier Supplier for result consumers.
     * @param resultParser Function to apply  a triple as twitter7 block reading result to a callable parser.
     * @throws IOException Can be thrown by errors during reader creation.
     * @throws NullPointerException Thrown if {@code callBackSupplier} or {@code resultParser} is {@code null}.
     */
    public Twitter7Reader(
            String fileToParsePath,
            Supplier<FutureCallback<T>> callbackSupplier,
            Function<Triple<String, String, String>, Callable<T>> resultParser) throws IOException, NullPointerException {
        this(new File(fileToParsePath), callbackSupplier, resultParser);
    }


    @Override
    public void run() throws IllegalStateException {
        if (this.running) {
            throw new IllegalStateException();
        }

        this.running = true;
        this.unfinishedFutures = new HashSet<>();
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        try {

            Triple<String, String, String> twitter7Block;
            while ((twitter7Block = this.readTwitter7Block()) != null) {
                ListenableFuture<T> fut = service.submit(this.resultParser.apply(twitter7Block));
                Futures.addCallback(fut, this.callbackSupplier.get());

                // Track futures
                synchronized (this.unfinishedFutures) {
                    this.unfinishedFutures.add(fut);
                    fut.addListener(() -> {
                        synchronized (this.unfinishedFutures) {
                            this.unfinishedFutures.remove(fut);
                        }
                    }, service);
                }
            }

        } catch (IOException e) {
            LOGGER.error("Encountered IOException during file parsing.");
        }
        this.running = false;
    }

    /**
     * Returns whether the reading of the file has finished or hasn't been started.
     * @return Returns {@code true} if there is no file reading ongoing.
     */
    public boolean isFinished() {
        return !this.running && this.unfinishedFutures.isEmpty();
    }

    /**
     * Reads the next block of twitter7 data.
     * @return Triple containing the three twitter7 data lines.
     * @throws IOException Thrown if exception occurs during file reading.
     */
    protected Triple<String, String, String> readTwitter7Block() throws IOException {

        MutableTriple<String, String, String> triple = new MutableTriple<>();
        READ_STATE readState = START_STATE;

        while (!this.readingFinished(readState)) {

            // Skip empty lines
            String line;
            while ((line = this.fileReader.readLine()) != null && line.isEmpty());
            if (line == null) {
                return null;
            }

            String linePrefix = lineIdentifier(readState);
            if (line.startsWith(linePrefix)) {
                triplePutLine(readState, triple).accept(line.substring(linePrefix.length()));
                readState = nextState(readState);
            } else {

                LOGGER.error("Encountered malformed block in twitter7 data.");

                // Skip non-empty lines
                while ((line = this.fileReader.readLine()) != null && !line.isEmpty());
                if (line == null) {
                    return null;
                }

                return readTwitter7Block();
            }
        }

        return triple;
    }

    /**
     * All states during one invoke of {@link #readTwitter7Block()}.
     */
    private enum READ_STATE {
        READ_T,
        READ_U,
        READ_W,
        READ_FINISHED
    }

    /**
     * Start state of {@link #readTwitter7Block()}.
     */
    private static final READ_STATE START_STATE = READ_STATE.READ_T;

    /**
     * Returns whether the given state is the finishing one.
     * @param state State to check.
     * @return True if parsing can be finished.
     */
    private boolean readingFinished(READ_STATE state) {
        switch (state) {
            case READ_FINISHED: return true;
        }

        return false;
    }

    /**
     * Returns the next state.
     * @param oldState Current state to get the next for.
     * @return Next state.
     * @throws IllegalStateException Thrown if {@code oldState} state has no next state.
     */
    private READ_STATE nextState(READ_STATE oldState) throws IllegalStateException {
        switch (oldState) {
            case READ_T: return READ_STATE.READ_U;
            case READ_U: return READ_STATE.READ_W;
            case READ_W: return READ_STATE.READ_FINISHED;
            default: throw new IllegalStateException();
        }
    }

    /**
     * Twitter7 data has lines starting with different prefixes. This method returns the line start for given state.
     * @param state State to get prefix for.
     * @return Prefix.
     * @throws IllegalStateException Thrown if {@code state} is finishing state.
     */
    private String lineIdentifier(READ_STATE state) throws IllegalStateException {
        switch (state) {
            case READ_T: return "T";
            case READ_U: return "U";
            case READ_W: return "W";
            default: throw new IllegalStateException();
        }
    }

    /**
     * Returns consumer to a line. This consumer will set the line to the right triple element.
     * @param state State to get the consumer for.
     * @param triple Triple to store the consumed result in.
     * @return Consumer that stores a line in the triple.
     * @throws IllegalStateException Thrown if {@code state} is finishing state.
     */
    private Consumer<String> triplePutLine(READ_STATE state, MutableTriple<String, String, String> triple) throws IllegalStateException {
        switch (state) {
            case READ_T: return line -> triple.setLeft(line);
            case READ_U: return line -> triple.setMiddle(line);
            case READ_W: return line -> triple.setRight(line);
            default: throw new IllegalStateException();
        }
    }
}
