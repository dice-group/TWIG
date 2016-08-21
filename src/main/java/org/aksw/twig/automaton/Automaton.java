package org.aksw.twig.automaton;

import org.aksw.twig.automaton.data.*;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.SamplingDiscreteDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
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

    private final SamplingWordPredecessorSuccessorDistribution samplingWordPredecessorSuccessorDistribution;

    private final SamplingDiscreteDistribution<Integer> tweetNumberDistribution;

    private final SamplingDiscreteDistribution<LocalTime> tweetTimeDistribution;

    public Automaton(SamplingWordPredecessorSuccessorDistribution samplingWordPredecessorSuccessorDistribution, SamplingDiscreteDistribution<Integer> tweetNumberDistribution, SamplingDiscreteDistribution<LocalTime> tweetTimeDistribution) {
        this.samplingWordPredecessorSuccessorDistribution = samplingWordPredecessorSuccessorDistribution;
        this.tweetNumberDistribution = tweetNumberDistribution;
        this.tweetTimeDistribution = tweetTimeDistribution;
    }

    public TWIGModelWrapper simulate(int userCount, Duration simulationTime, LocalDate startDate, long seed) {

        Random r = new Random(seed);

        samplingWordPredecessorSuccessorDistribution.reseedRandomGenerator(seed);
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
                String tweetContent = samplingWordPredecessorSuccessorDistribution.sample();

                resultModel.addTweetNoAnonymization(user.getNameAsHexString(), tweetContent, tweetTime, Collections.emptyList()); // TODO: use mentions
            }
        }

        return resultModel;
    }

    public static void main(String[] args) {

        if (args.length < 8) {
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

        File f = new File(args[7]);
        if (!f.isDirectory()) {
            throw new IllegalArgumentException("Supplied file must be a directory");
        }

        Automaton automaton = new Automaton(new WordSampler(wordMatrix), messageCounter.normalized(Duration.ofDays(TWEET_NUMBER_NORMALIZATION)).getValueDistribution(), timeCounter.getValueDistribution());
        TWIGModelWrapper modelWrapper = automaton.simulate(userCount, Duration.ofDays(days), startDate, seed);

        try (FileWriter writer = new FileWriter(new FileHandler(f, "generated_twig_model", ".ttl").nextFile())) {
            modelWrapper.write(writer);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
