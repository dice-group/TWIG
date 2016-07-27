package org.aksw.twig.automaton.time;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.Set;

public class TimeCounter {

    private static final Logger LOGGER = LogManager.getLogger(TimeCounter.class);

    private static final int HOURS = 24;

    private static final int MINUTES = 60;

    private long[][] tweetTimes = new long[HOURS][MINUTES];

    public long[][] getTweetTimes() {
        return tweetTimes;
    }

    public long getTweetTimesAt(int hour, int minute) {
        return tweetTimes[hour][minute];
    }

    public void addModel(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getPredicate().getLocalName().equals(Twitter7ModelWrapper.TWEET_TIME_PROPERTY_NAME)) {
                Literal literal = statement.getObject().asLiteral();
                LocalDateTime time = LocalDateTime.from(Twitter7ModelWrapper.DATE_TIME_FORMATTER.parse(literal.getString()));
                addTweetTime(time, 1);
            }
        });
    }

    public void addTweetTime(LocalDateTime time, long count) {
        tweetTimes[time.getHour()][time.getMinute()] += count;
    }

    public static void main(String[] args) {

        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);

        TimeCounter counter = new TimeCounter();
        fileArgs.getRight().forEach(file -> {
            try {
                Twitter7ModelWrapper modelWrapper = Twitter7ModelWrapper.read(file);
                counter.addModel(modelWrapper.getModel());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "time_counter", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(counter.getTweetTimes());
            objectOutputStream.flush();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
