package org.aksw.twig.parsing;

import com.google.common.util.concurrent.*;
import org.aksw.twig.files.FileHandler;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class reads a file that contains twitter7 data block-wise.
 * Each block must be formatted by following regex:<br/>
 *      <ul><li>{T .*[\n]+
 *      U .*[\n]+
 *      W .*[\n]}+}*</li></ul>
 * Blocks that do not match this criteria will be skipped.
 *
 * @param <T> Type of the parsing result.
 * @author Felix Linker
 */
class Twitter7Reader<T> {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Reader.class);

    private File fileToParse;

    private Supplier<FutureCallback<T>> callbackSupplier;

    private Function<Triple<String, String, String>, Callable<T>> resultParser;

    private static final int N_THREADS = 32;

    private final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(N_THREADS));

    /**
     * Initializes a file reader to given file and sets class variables.
     * @param fileToParse File to parse.
     * @param callbackSupplier Supplier for result consumers.
     * @param resultParser Function to apply  a triple as twitter7 block reading result to a callable parser.
     * @throws IOException Can be thrown by errors during reader creation.
     * @throws NullPointerException Thrown if {@code callBackSupplier} or {@code resultParser} is {@code null}.
     */
    Twitter7Reader(
            File fileToParse,
            Supplier<FutureCallback<T>> callbackSupplier,
            Function<Triple<String, String, String>, Callable<T>> resultParser) throws IOException, NullPointerException {
        if (callbackSupplier == null || resultParser == null) {
            throw new NullPointerException();
        }

        this.fileToParse = fileToParse;
        this.callbackSupplier = callbackSupplier;
        this.resultParser = resultParser;
    }

    /**
     * Starts reading of the given file. If you want to read the same file twice you cannot do this with the same object.
     * @throws IllegalStateException Thrown if reader gets started twice.
     */
    void read() throws IllegalStateException {
        if (service.isShutdown()) {
            throw new IllegalStateException();
        }

        try (BufferedReader fileReader = FileHandler.getDecodingReader(this.fileToParse)) {

            Triple<String, String, String> twitter7Block;
            while ((twitter7Block = readTwitter7Block(fileReader)) != null) {
                ListenableFuture<T> fut = service.submit(resultParser.apply(twitter7Block));
                Futures.addCallback(fut, callbackSupplier.get());
            }

        } catch (IOException e) {
            LOGGER.error("Encountered IOException during file parsing.");
        }

        this.service.shutdown();
    }

    /**
     * Returns whether the reading of the file has finished or hasn't been started.
     * @return Returns {@code true} if there is no file reading ongoing.
     */
    boolean isFinished() {
        return service.isTerminated();
    }

    /**
     * Reads the next block of twitter7 data.
     * @return Triple containing the three twitter7 data lines.
     * @throws IOException Thrown if exception occurs during file reading.
     */
    Triple<String, String, String> readTwitter7Block(BufferedReader fileReader) throws IOException {

        MutableTriple<String, String, String> triple = new MutableTriple<>();
        READ_STATE readState = START_STATE;

        while (!this.readingFinished(readState)) {

            // Skip empty lines
            String line;
            while ((line = fileReader.readLine()) != null && line.isEmpty());
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
                while ((line = fileReader.readLine()) != null && !line.isEmpty());
                if (line == null) {
                    return null;
                }

                return readTwitter7Block(fileReader);
            }
        }

        return triple;
    }

    /**
     * All states during one invoke of {@link #readingFinished(READ_STATE)}.
     */
    private enum READ_STATE {
        READ_T,
        READ_U,
        READ_W,
        READ_FINISHED
    }

    /**
     * Start state of {@link #readingFinished(READ_STATE)}.
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
            case READ_T: return triple::setLeft;
            case READ_U: return triple::setMiddle;
            case READ_W: return triple::setRight;
            default: throw new IllegalStateException();
        }
    }
}
