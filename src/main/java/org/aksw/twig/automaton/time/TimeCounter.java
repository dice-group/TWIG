package org.aksw.twig.automaton.time;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.aksw.twig.statistics.DiscreteDistribution;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Counts the timestamp of tweets per day. Intended to be used with a Twitter7Model as it can be created by {@link Twitter7ModelWrapper}.
 */
public class TimeCounter implements Serializable {

    private static final long serialVersionUID = 4946607680737050029L;

    private static transient final Logger LOGGER = LogManager.getLogger(TimeCounter.class);

    private static final int HOURS = 24;

    private static final int MINUTES = 60;

    private long[][] tweetTimes = new long[HOURS][MINUTES];

    /**
     * Returns a matrix of tweet times with following semantics:
     * {@code x := matrix[h][m]} means that {@code x} messages have been sent between {@code h:m} o'clock (inclusive) and {@code h:(m+1)} o'clock (exclusive).
     * @return Matrix with values.
     */
    public long[][] getTweetTimes() {
        return tweetTimes;
    }

    /**
     * Returns number of tweets at given time. Equivalent to {@code matrix[hour][minute]} in {@link #getTweetTimes()}.
     * @param hour Hour.
     * @param minute Minute.
     * @return Number of tweets.
     */
    public long getTweetTimesAt(int hour, int minute) {
        return tweetTimes[hour][minute];
    }

    /**
     * Adds all timestamps from the model.
     * @param model Model to add.
     */
    public void addModel(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getPredicate().getLocalName().equals(Twitter7ModelWrapper.TWEET_TIME_PROPERTY_NAME)) {
                Literal literal = statement.getObject().asLiteral();
                LocalDateTime time = LocalDateTime.from(Twitter7ModelWrapper.DATE_TIME_FORMATTER.parse(literal.getString()));
                addTweetTime(time, 1);
            }
        });
    }

    /**
     * Adds {@code count} messages to the given time. Only hours and minutes will be considered.
     * @param time Time to add to.
     * @param count Messages to add.
     */
    public void addTweetTime(LocalDateTime time, long count) {
        tweetTimes[time.getHour()][time.getMinute()] += count;
    }

    /**
     * Adds all time counts from given {@link TimeCounter} to this.
     * @param timeCounter Time counter to add from.
     */
    public void merge(TimeCounter timeCounter) {
        for (int h = 0; h < HOURS; h++) {
            for (int m = 0; m < MINUTES; m++) {
                tweetTimes[h][m] += timeCounter.tweetTimes[h][m];
            }
        }
    }

    public DiscreteDistribution<LocalTime> getValueDistribution() {
        DiscreteDistribution<LocalTime> distribution = new DiscreteDistribution<>();

        double sum = 0;
        for (int h = 0; h < HOURS; h++) {
            for (int m = 0; m < MINUTES; m++) {
                sum += (double) tweetTimes[h][m];
            }
        }

        for (int h = 0; h < HOURS; h++) {
            for (int m = 0; m < MINUTES; m++) {
                LocalTime time = LocalTime.of(h, m);
                distribution.addDiscreteEvent(time, (double) tweetTimes[h][m] / sum);
            }
        }

        return distribution;
    }

    /**
     * Creates a timestamp matrix as stated in {@link #getTweetTimes()} by adding given files as {@link Model} and writes it into a file.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * @param args Arguments.
     */
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

        for (int hour = 0; hour < HOURS; hour++) {
            int hourCount = 0;
            for (int minute = 0; minute < MINUTES; minute++) {
                hourCount += counter.getTweetTimesAt(hour, minute);
            }
            LOGGER.info("Between {}h and {}h {} messages have been sent.", hour, hour + 1, hourCount);
        }

        if (fileArgs.getLeft() == null) {
            return;
        }

        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "time_counter", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(counter);
            objectOutputStream.flush();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
