package org.aksw.twig.automaton.data;

import org.aksw.twig.executors.FileReadingSuspendSupplier;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Set;
import java.util.concurrent.Callable;

public class MessageCounterHandler extends FileReadingSuspendSupplier<MessageCounter> {

    private static final Logger LOGGER = LogManager.getLogger(MessageCounterHandler.class);

    private final MessageCounter mergedResult = new MessageCounter();

    public MessageCounterHandler(Set<File> filesToParse) {
        super(filesToParse);
    }

    @Override
    public Callable<MessageCounter> getFileProcessor(File file) {
        return () -> {
            LOGGER.info("Parsing file {}", file.getName());
            MessageCounter counter = new MessageCounter();
            counter.addModel(Twitter7ModelWrapper.read(file).getModel());
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

    public static void main(String[] args) {
        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        MessageCounterHandler handler = new MessageCounterHandler(fileArgs.getRight());
        FileReadingSuspendSupplier.start("message_count.obj", fileArgs.getLeft(), handler);
    }
}
