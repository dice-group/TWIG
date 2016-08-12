package org.aksw.twig.automaton.time;

import org.aksw.twig.automaton.messageCount.MessageCounter;
import org.aksw.twig.automaton.messageCount.MessageCounterHandler;
import org.aksw.twig.executors.SelfSuspendingExecutor;
import org.aksw.twig.executors.SuspendSupplier;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

public class TimeCounterHandler implements SuspendSupplier<TimeCounter> {

    private static final Logger LOGGER = LogManager.getLogger(TimeCounterHandler.class);

    private final TimeCounter mergedResult = new TimeCounter();

    private final NavigableSet<File> filesToParse = new TreeSet<>();

    public TimeCounterHandler(Collection<File> filesToParse) {
        this.filesToParse.addAll(filesToParse);
    }

    @Override
    public Callable<TimeCounter> next() {
        if (filesToParse.isEmpty()) {
            return null;
        }

        LOGGER.info("Supplying callable");
        synchronized (filesToParse) {
            File fileToParse = filesToParse.first();
            filesToParse.remove(fileToParse);

            return () -> {
                LOGGER.info("Parsing file {}", fileToParse.getName());
                TimeCounter counter = new TimeCounter();
                counter.addModel(Twitter7ModelWrapper.read(fileToParse).getModel());
                return counter;
            };
        }
    }

    @Override
    public void addResult(TimeCounter result) {
        synchronized (mergedResult) {
            LOGGER.info("Merging result");
            mergedResult.merge(result);
        }
    }

    /**
     * Executes reading multiple models from files and parses them for message counting as stated in {@link MessageCounter}.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * @param args Arguments to the program.
     */
    public static void main(String[] args) {

        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "time_count", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        TimeCounterHandler handler = new TimeCounterHandler(fileArgs.getRight());
        SelfSuspendingExecutor<TimeCounter> executor = new SelfSuspendingExecutor<>(handler);
        executor.addFinishedEventListeners(() -> {
            if (outputFile != null) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                    objectOutputStream.writeObject(handler.mergedResult);
                    objectOutputStream.flush();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
        LOGGER.info("Starting executor");
        executor.start();
    }
}
