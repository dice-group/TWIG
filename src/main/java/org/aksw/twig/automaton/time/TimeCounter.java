package org.aksw.twig.automaton.time;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

public class TimeCounter {

    private static final Logger LOGGER = LogManager.getLogger(TimeCounter.class);

    private static final Query MESSAGE_TIME_QUERY = QueryFactory.create(
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX twig: <http://aksw.org/twig#> " +
            "SELECT ?time (COUNT(?msg) AS ?count) WHERE { " +
                    "?msg rdf:type twig:Tweet . " +
                    "?msg twig:tweetTime ?time" +
            "} GROUP BY ?time"
    );

    private long[] tweetTimes = new long[24 * 60];

    public long getTweetTimesAt(int i) {
        return tweetTimes[i];
    }

    public void addTweetTime(LocalDateTime time, long count) {
        int index = time.getHour() * 60 + time.getMinute();
        tweetTimes[index] += count;
    }

    public static void main(String[] args) {

        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        /*File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "time_counter", ".obj").nextFile();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        TimeCounter counter = new TimeCounter();
        fileArgs.getRight().forEach(file -> {
            try {
                Twitter7ModelWrapper modelWrapper = Twitter7ModelWrapper.read(file);
                try (QueryExecution queryExecution = QueryExecutionFactory.create(MESSAGE_TIME_QUERY, modelWrapper.getModel())) {
                    queryExecution.execSelect().forEachRemaining(querySolution -> {
                        String timeText = querySolution.getLiteral("time").getString();
                        LocalDateTime time = LocalDateTime.from(Twitter7ModelWrapper.DATE_TIME_FORMATTER.parse(timeText));
                        counter.addTweetTime(time, querySolution.getLiteral("count").getLong());
                    });
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                return;
            }
        });

        for (int i = 0; i < 24; i++) {
            long hourCount = 0;
            for (int j = 0; j < 60; j++) {
                hourCount += counter.getTweetTimesAt(i * 60 + j);
            }
            LOGGER.info("{} messages have been sent between {}h and {}h", hourCount, i, (i + 1) % 24);
        }
    }
}
