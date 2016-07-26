package org.aksw.twig.parsing;

import com.google.common.util.concurrent.*;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.GZIPOutputStream;

/**
 * This class reads a file that contains twitter7 data block-wise.
 * Each block must be formatted by following regex:<br/>
 *      <ul><li>{T .*[\n]+
 *      U .*[\n]+
 *      W .*[\n]}+}*</li></ul>
 * Blocks that do not match this criteria will be skipped.
 *
 * @author Felix Linker
 */
public class Twitter7Parser implements FutureCallback<TwitterModelWrapper>, Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Twitter7Parser.class);

    private static final int N_THREADS = 32;

    private static final String FILE_TYPE = ".ttl.gz";

    private static final int MODEL_MAX_SIZE = 1000000;

    private Function<Triple<String, String, String>, Callable<TwitterModelWrapper>> resultParser;

    private final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(N_THREADS));

    private final TwitterModelWrapper currentModel = new TwitterModelWrapper();

    private final FileHandler fileHandler;

    private final BufferedReader fileReader;

    private boolean run = false;

    private boolean finished = false;

    /**
     * Initializes a file reader to given file and sets class variables.
     * @param fileToParse File to parse.
     * @param resultParser Function to apply  a triple as twitter7 block reading result to a callable parser.
     * @throws IOException Can be thrown by errors during reader creation.
     * @throws NullPointerException Thrown if {@code callBackSupplier} or {@code resultParser} is {@code null}.
     */
    public Twitter7Parser(
            File fileToParse,
            File outputDirectory,
            Function<Triple<String, String, String>, Callable<TwitterModelWrapper>> resultParser) throws IOException, NullPointerException {
        if (resultParser == null || fileToParse == null || outputDirectory == null) {
            throw new NullPointerException();
        }

        if (!outputDirectory.isDirectory()) {
            throw new IllegalArgumentException();
        }

        this.resultParser = resultParser;
        String fileName = fileToParse.getName();
        int nameEndIndex = fileName.indexOf('.');
        fileName = fileName.substring(0, nameEndIndex == -1 ? fileName.length() : nameEndIndex);
        this.fileHandler = new FileHandler(outputDirectory, fileName, FILE_TYPE);
        this.fileReader = new BufferedReader(new InputStreamReader(FileHandler.getDecompressionStreams(fileToParse)));
    }

    /**
     * Starts reading of the given file. If you want to read the same file twice you cannot do this with the same object.
     * @throws IllegalStateException Thrown if reader gets started twice.
     */
    @Override
    public void run() {
        if (run) {
            throw new IllegalStateException();
        }
        run = true;

        for (int i = 0; i < N_THREADS; i++) {
            readTwitter7Block();
        }
    }

    /**
     * Returns whether the reading of the file has finished or hasn't been started.
     * @return Returns {@code true} if there is no file reading ongoing.
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Reads the next block of twitter7 data and hands it to the executor service.
     */
    private void readTwitter7Block() {
        if (service.isShutdown()) {
            return;
        }

        synchronized (fileReader) {

            recursion:
            while (true) { // mimics recursion iteratively
                MutableTriple<String, String, String> triple = new MutableTriple<>();
                READ_STATE readState = START_STATE;

                try {
                    while (!readingFinished(readState)) {

                        // Skip empty lines
                        String line;
                        while ((line = fileReader.readLine()) != null && line.isEmpty());
                        if (line == null) {
                            finish();
                            return;
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
                                finish();
                                return;
                            }

                            continue recursion; // "recursive" call
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                    continue; // "recursive" call
                }

                ListenableFuture<TwitterModelWrapper> fut = service.submit(resultParser.apply(triple));
                Futures.addCallback(fut, this);
                return;
            }
        }
    }

    @Override
    public void onSuccess(TwitterModelWrapper result) {
        synchronized (currentModel) {
            currentModel.getModel().add(result.getModel());

            if (currentModel.getModel().size() >= MODEL_MAX_SIZE) {
                writeModel();
            }
        }

        readTwitter7Block();
    }

    @Override
    public void onFailure(Throwable t) {
        LOGGER.error(t.getMessage(), t);
        readTwitter7Block();
    }

    /**
     * Writes the current collected model into a file.
     */
    private void writeModel() {
        synchronized (currentModel) {
            LOGGER.info("Writing result model {}.", currentModel);

            try (FileOutputStream fileOutputStream = new FileOutputStream(this.fileHandler.nextFile())) {
                try (Writer writer = new OutputStreamWriter(new GZIPOutputStream(fileOutputStream))) {
                    currentModel.write(writer);
                    writer.flush();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
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

    private void finish() {
        if (service.isShutdown()) {
            throw new IllegalStateException();
        }

        // Close file reader
        synchronized (fileReader) {
            try {
                fileReader.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        // Shutdown threaded execution service
        service.shutdown();
        while (!service.isTerminated()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        writeModel();
        finished = true;
    }

    /**
     * Parses one or more files according to twitter7 format. Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * You should not parse files with the same name from different directories as that could mess up the output.
     * @param args One or more arguments as specified above.
     * @see Twitter7Parser
     */
    public static void main(String[] args) {

        Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);
        File outputDirectory = parsedArgs.getLeft();
        Set<File> filesToParse = parsedArgs.getRight();

        // Start parsing
        final ExecutorService service = Executors.newFixedThreadPool(filesToParse.size());
        filesToParse.forEach(file -> {
            try {
                service.execute(new Twitter7Parser(file, outputDirectory, Twitter7BlockParser::new));
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
        service.shutdown();
    }
}
