package org.aksw.twig.automaton.messageCount;

import org.aksw.twig.executors.SuspendSupplier;
import org.aksw.twig.executors.SelfSuspendingExecutor;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * Handles concurrent reading of multiple files as Twitter7Models as provided by {@link Twitter7ModelWrapper}.
 */
public class MessageCounterHandler implements SuspendSupplier<MessageCounter> {

    private static final Logger LOGGER = LogManager.getLogger(MessageCounterHandler.class);

    private final MessageCounter messageCounter = new MessageCounter();

    private final NavigableSet<File> filesToParse = new TreeSet<>();

    /**
     * Creates a new instance setting class variables.
     * @param filesToParse Files to read the models from.
     */
    public MessageCounterHandler(Set<File> filesToParse) {
        this.filesToParse.addAll(filesToParse);
    }

    @Override
    public Callable<MessageCounter> next() {
        if (filesToParse == null || filesToParse.isEmpty()) {
            return null;
        }

        synchronized (filesToParse) {
            File fileToParse = filesToParse.first();
            filesToParse.remove(fileToParse);

            return () -> {
                MessageCounter counter = new MessageCounter();
                counter.addModel(Twitter7ModelWrapper.read(fileToParse).getModel());
                return counter;
            };
        }
    }

    @Override
    public void addResult(MessageCounter result) {
        synchronized (messageCounter) {
            messageCounter.merge(result);
        }
    }

    /**
     * Executes reading multiple models from files and parses them for message counting as stated in {@link MessageCounter}.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * @param args Arguments to the program.
     */
    public static void main(String[] args) {

        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        File outputFile = fileArgs.getLeft();
        MessageCounterHandler handler = new MessageCounterHandler(fileArgs.getRight());
        SelfSuspendingExecutor<MessageCounter> executor = new SelfSuspendingExecutor<>(handler);
        executor.addFinishedListener(() -> {
            handler.messageCounter.logResults();

            if (outputFile != null) {
                try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                    objectOutputStream.writeObject(handler.messageCounter);
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        });
    }
}
