package org.aksw.twig.automaton;

import org.aksw.twig.automaton.data.MessageCounter;
import org.aksw.twig.automaton.data.TimeCounter;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.DiscreteDistribution;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Random;

public class Automaton {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final int SECONDS = 60;

    private static final int TWEET_NUMBER_NORMALIZATION = 30;

    private final WordSampler wordSampler;

    private final ExponentialLikeDistribution tweetNumberDistribution;

    private final DiscreteDistribution<LocalTime> tweetTimeDistribution;

    public Automaton(WordSampler wordSampler, ExponentialLikeDistribution tweetNumberDistribution, DiscreteDistribution<LocalTime> tweetTimeDistribution) {
        this.wordSampler = wordSampler;
        this.tweetNumberDistribution = tweetNumberDistribution;
        this.tweetTimeDistribution = tweetTimeDistribution;
    }

    public TWIGModelWrapper simulate(int userCount, Duration simulationTime, LocalDate startDate, long seed) {

        Random r = new Random(seed);

        wordSampler.reseedRandomGenerator(seed);
        tweetNumberDistribution.reseedRandomGenerator(seed);
        tweetTimeDistribution.reseedRandomGenerator(seed);
        int simulationDays = (int) simulationTime.toDays();

        TWIGModelWrapper resultModel = new TWIGModelWrapper();

        for (int i = 0; i < userCount; i++) {
            User user = new User(tweetNumberDistribution.sample());
            user.setNameOfRandom(r);

            for (int d = 0; d < simulationDays; d++) {
                int tweetDay = r.nextInt(simulationDays);
                LocalDateTime tweetTime = LocalDateTime.of(startDate.plusDays(tweetDay), tweetTimeDistribution.sample().withSecond(r.nextInt(SECONDS))); // TODO: there can be collisions
                String tweetContent = wordSampler.sampleTweet();

                resultModel.addTweetNoAnonymization(user.getNameAsHexString(), tweetContent, tweetTime, Collections.emptyList()); // TODO: use mentions
            }
        }

        return resultModel;
    }

    public static void main(String[] args) {

        if (args.length < 7) {
            throw new IllegalArgumentException("Insufficient arguments supplied");
        }

        WordMatrix wordMatrix;
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(args[0]))) {
            wordMatrix = (WordMatrix) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        MessageCounter messageCounter;
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(args[1]))) {
            messageCounter = (MessageCounter) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        TimeCounter timeCounter;
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(args[2]))) {
            timeCounter = (TimeCounter) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        int userCount = Integer.parseInt(args[3]);
        int days = Integer.parseInt(args[4]);
        LocalDate startDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(args[5]));
        long seed = Long.parseLong(args[6]);

        Automaton automaton = new Automaton(new WordSampler(wordMatrix), messageCounter.normalized(Duration.ofDays(TWEET_NUMBER_NORMALIZATION)).getValueDistribution(), timeCounter.getValueDistribution());
        automaton.simulate(userCount, Duration.ofDays(days), startDate, seed);
    }
}
