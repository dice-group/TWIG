package org.aksw.twig.automaton.data;

import org.aksw.twig.executors.FileReadingSuspendSupplier;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.SamplingDiscreteTreeDistribution;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Creates multiple {@link TimeCounter} objects by parsing files as {@link TWIGModelWrapper} and adding them to a time counter.
 * Parsed objects will then be merged into one result.
 */
public class TimeCounterHandler extends FileReadingSuspendSupplier<TimeCounter, SamplingDiscreteTreeDistribution<LocalTime>> {

    private static final Logger LOGGER = LogManager.getLogger(TimeCounterHandler.class);

    private final TimeCounter mergedResult = new TimeCounter();

    /**
     * Creates a new object setting class variables.
     * @param filesToParse Files to parse.
     */
    public TimeCounterHandler(Collection<File> filesToParse) {
        super(filesToParse);
    }

    @Override
    public Callable<TimeCounter> getFileProcessor(File file) {
        return () -> {
            LOGGER.info("Parsing file {}", file.getName());
            TimeCounter counter = new TimeCounter();
            counter.addModel(TWIGModelWrapper.read(file).getModel());
            return counter;
        };
    }

    @Override
    public void addResult(TimeCounter result) {
        synchronized (mergedResult) {
            LOGGER.info("Merging result");
            mergedResult.merge(result);
        }
    }

    @Override
    public SamplingDiscreteTreeDistribution<LocalTime> getMergedResult() {
        return mergedResult.getValueDistribution();
    }

    /**
     * Executes reading multiple models from files and parses them for message counting as stated in {@link MessageCounter}.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * @param args Arguments to the program.
     */
    public static void main(String[] args) {
        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        TimeCounterHandler handler = new TimeCounterHandler(fileArgs.getRight());
        FileReadingSuspendSupplier.start("time_count_distribution.obj", fileArgs.getLeft(), handler);
    }
}
