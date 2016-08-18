package org.aksw.twig.automaton.data;

import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.DiscreteDistribution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Holds a frequency distribution of timestamps with hour and minute.
 */
public class TimeCounter implements Serializable {

    private static final long serialVersionUID = 4946607680737050029L;

    private static final int HOURS = 24;

    private static final int MINUTES = 60;

    private long[][] tweetTimes = new long[HOURS][MINUTES];

    /**
     * Returns a matrix of hours and minutes with following semantics:
     * {@code x := matrix[h][m]} means that {@code x} timestamps have been counted between {@code h:m} o'clock (inclusive) and {@code h:(m+1)} o'clock (exclusive).
     * @return Matrix with values.
     */
    public long[][] getTweetTimes() {
        return tweetTimes;
    }

    /**
     * Returns number of timestamps at given time. Equivalent to {@code matrix[hour][minute]} in {@link #getTweetTimes()}.
     * @param hour Hour.
     * @param minute Minute.
     * @return Number of tweets.
     */
    public long getTimesCountAt(int hour, int minute) {
        return tweetTimes[hour][minute];
    }

    /**
     * Adds all timestamps of tweets from the model by iterating over all statements.
     * @param model Model to add.
     */
    public void addModel(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getPredicate().getLocalName().equals(TWIGModelWrapper.TWEET_TIME_PROPERTY_NAME)) {
                Literal literal = statement.getObject().asLiteral();
                LocalDateTime time = LocalDateTime.from(TWIGModelWrapper.DATE_TIME_FORMATTER.parse(literal.getString()));
                addTimestamps(time, 1);
            }
        });
    }

    /**
     * Adds {@code count} timestamps to the given time. Only hours and minutes will be considered.
     * @param time Time to add to.
     * @param count Count to add.
     */
    public void addTimestamps(LocalDateTime time, long count) {
        tweetTimes[time.getHour()][time.getMinute()] += count;
    }

    /**
     * Adds all timestamp counts from given {@link TimeCounter} to this.
     * @param timeCounter Time counter to add from.
     */
    public void merge(TimeCounter timeCounter) {
        for (int h = 0; h < HOURS; h++) {
            for (int m = 0; m < MINUTES; m++) {
                tweetTimes[h][m] += timeCounter.tweetTimes[h][m];
            }
        }
    }

    /**
     * Creates a discrete distribution frequency measure by the timestamp counts as frequency distribution.
     * @return Discrete distribution.
     */
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
}
