package org.aksw.twig.parsing;

import com.google.common.util.concurrent.*;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Twitter7Reader implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Reader.class);

    private BufferedReader fileReader;

    private Supplier<FutureCallback<String>> callbackSupplier;

    /**
     * Initializes a file reader to given file and sets class variables.
     * @param fileToParse File to parse.
     * @param callbackSupplier Supplier for result consumers.
     * @throws IOException Can be thrown by errors during reader creation.
     */
    public Twitter7Reader(File fileToParse, Supplier<FutureCallback<String>> callbackSupplier) throws IOException {
        this.fileReader = new BufferedReader(new FileReader(fileToParse));
        this.callbackSupplier = callbackSupplier;
    }

    /**
     * Initializes a file reader to given file and sets class variables.
     * @param fileToParsePath File to parse.
     * @param callbackSupplier Supplier for result consumers.
     * @throws IOException Can be thrown by errors during reader creation.
     */
    public Twitter7Reader(String fileToParsePath, Supplier<FutureCallback<String>> callbackSupplier) throws IOException {
        this(new File(fileToParsePath), callbackSupplier);
    }

    @Override
    public void run() {
        ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

        try {

            Triple<String, String, String> twitter7Block;
            while ((twitter7Block = this.readTwitter7Block()) != null) {
                ListenableFuture<String> fut = service.submit(new Twitter7BlockParser(twitter7Block.getLeft(), twitter7Block.getMiddle(), twitter7Block.getRight()));
                Futures.addCallback(fut, this.callbackSupplier.get());
            }

        } catch (IOException e) {
            LOGGER.error("Encountered IOException during file parsing.");
            return;
        }
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

            if (line.startsWith(lineIdentifier(readState))) {
                triplePutLine(readState, triple).accept(line);
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
