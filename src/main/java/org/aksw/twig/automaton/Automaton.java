package org.aksw.twig.automaton;

import org.aksw.twig.automaton.data.MessageCounter;
import org.aksw.twig.automaton.data.TimeCounter;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Automaton {

    private static final Logger LOGGER = LogManager.getLogger();

    private final WordSampler wordSampler;

    private final MessageCounter messageCounter;

    private final DiscreteDistribution<LocalTime> messageTimeDistribution;

    private final UserFactory userFactory = new UserFactory();

    private final List<User> users = new LinkedList<>();

    public Automaton(WordMatrix wordMatrix, MessageCounter messageCounter, DiscreteDistribution<LocalTime> messageTimeDistribution) {
        wordSampler = new WordSampler(wordMatrix);
        this.messageCounter = messageCounter;
        this.messageTimeDistribution = messageTimeDistribution;
    }

    public void simulate(int userCount, Duration simulationTime, LocalDate startDate, long seed) {

        Random r = new Random(seed);
        final ExponentialLikeDistribution messageCountDistribution = messageCounter.normalized(simulationTime).getValueDistribution();
        messageCountDistribution.reseedRandomGenerator(seed);
        wordSampler.reseedRandomGenerator(seed);
        messageTimeDistribution.reseedRandomGenerator(seed);
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

        Automaton automaton = new Automaton(wordMatrix, messageCounter, timeCounter.getValueDistribution());
        automaton.simulate(userCount, Duration.ofDays(days), startDate, seed);
    }
}
