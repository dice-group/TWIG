package org.aksw.twig.automaton;

import org.aksw.twig.automaton.data.MessageCounter;
import org.aksw.twig.automaton.data.TimeCounter;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.statistics.SamplingDiscreteDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

public class AutomatonTest {

    private static final Logger LOGGER = LogManager.getLogger(AutomatonTest.class);

//    @Test
    public void AutomatonTest() {

        MessageCounter messageCounter;
        TimeCounter timeCounter;
        WordMatrix wordMatrix;

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("C:/users/felix/desktop/TWIG_test/message_count_0.obj"))) {
            messageCounter = (MessageCounter) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail();
            return;
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("C:/users/felix/desktop/TWIG_test/time_count_0.obj"))) {
            timeCounter = (TimeCounter) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail();
            return;
        }

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("C:/users/felix/desktop/TWIG_test/word_matrix_0.obj"))) {
            wordMatrix = (WordMatrix) inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            Assert.fail();
            return;
        }

        SamplingDiscreteDistribution<Integer> messageDistribution = messageCounter.getValueDistribution();
        SamplingDiscreteDistribution<LocalTime> timeDistribution = timeCounter.getValueDistribution();
        WordSampler wordSampler = new WordSampler(wordMatrix);

        Automaton automaton = new Automaton(wordSampler, messageDistribution, timeDistribution, new File("C:/users/felix/desktop/TWIG_test_output"));
        automaton.simulate(50, Duration.ofDays(60), LocalDate.of(2010, 1, 1), 1000);
    }
}
