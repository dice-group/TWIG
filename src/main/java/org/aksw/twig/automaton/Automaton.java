package org.aksw.twig.automaton;

import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.statistics.DiscreteDistribution;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Automaton {

    private static final Logger LOGGER = LogManager.getLogger();

    private final WordSampler wordSampler;

    private final ExponentialLikeDistribution messageCountDistribution;

    private final DiscreteDistribution<LocalTime> messageTimeDistribution;

    private final UserFactory userFactory = new UserFactory();

    private final List<User> users = new LinkedList<>();

    public Automaton(WordMatrix wordMatrix, ExponentialLikeDistribution messageCountDistribution, DiscreteDistribution<LocalTime> messageTimeDistribution) {
        wordSampler = new WordSampler(wordMatrix);
        this.messageCountDistribution = messageCountDistribution;
        this.messageTimeDistribution = messageTimeDistribution;
    }

    public void simulate(int userCount, Duration simulationTime, LocalDate startDate) {

        long seed = simulationTime.hashCode() * userCount;
        Random r = new Random(seed);
        messageCountDistribution.reseedRandomGenerator(seed);
        wordSampler.reseed(seed);
        messageTimeDistribution.reseed(seed);
        long simulationDays = simulationTime.toDays();

        for (int i = 0; i < userCount; i++) {
            User user = userFactory.newUser(messageCountDistribution.sample());
            user.setDays(simulationDays);
            users.add(user);
        }

        for (long d = 0; d < simulationDays; d++) {
            for (User user: users) {
                int messagesToSend = user.nextDay();
                for (int m = 0; m < messagesToSend; m++) {
                    LocalTime messageTime = messageTimeDistribution.sample();
                    messageTime = messageTime.withSecond((int) Math.ceil(r.nextDouble() * 60));
                    String tweet = wordSampler.sampleTweet();
                    LOGGER.info(
                            "T  {}\n" +
                            "U  {}\n" +
                            "W  {}\n",
                            LocalDateTime.of(startDate.plusDays(d), messageTime).toString(),
                            user.getName(),
                            tweet
                    );
                }
            }
        }
    }
}
