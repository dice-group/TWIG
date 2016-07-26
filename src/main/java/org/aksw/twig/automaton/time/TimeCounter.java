package org.aksw.twig.automaton.time;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TwitterModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public class TimeCounter {

    private static final Logger LOGGER = LogManager.getLogger(TimeCounter.class);

    private static final Query MESSAGE_TIME_QUERY = QueryFactory.create(

    );

    public static void main(String[] args) {

        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "time_counter", ".obj").nextFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileArgs.getRight().forEach(file -> {
            try {
                TwitterModelWrapper modelWrapper = TwitterModelWrapper.read(file);
                try (QueryExecution queryExecution = QueryExecutionFactory.create(MESSAGE_TIME_QUERY, modelWrapper.model)) {
                    ResultSet resultSet = queryExecution.execSelect();
                    // TODO
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });
    }
}
