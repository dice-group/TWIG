package org.aksw.twig.automaton;

import org.aksw.twig.automaton.data.MessageCounter;
import org.aksw.twig.automaton.data.TimeCounter;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SamplingDiscreteTreeDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalTime;

public class TmpMainClass {

    private static final Logger LOGGER = LogManager.getLogger(TmpMainClass.class);

    /**
     * arg[0] folder
     * args[1] wordmatrix
     * arg[2] messagecounter
     * arg[3] timecounter
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 4) {
            throw new IllegalArgumentException("Insufficient arguments");
        }

        File outputFolder = new File(args[0]);
        if (!outputFolder.isDirectory()) {
            throw new IllegalArgumentException("arg[0] must state a folder");
        }

        WordSampler sampler;
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(new File(args[1])))) {
            WordMatrix matrix = (WordMatrix) objectInputStream.readObject();
            sampler = new WordSampler(matrix);
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try {
            File outputFile = new FileHandler(outputFolder, "word_sampler", ".obj").nextFile();
            try (ObjectOutputStream outputStream  = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                outputStream.writeObject(sampler);
                outputStream.flush();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        ExponentialLikeDistribution messageDistribution;
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(args[2]))) {
            MessageCounter messageCounter = (MessageCounter) objectInputStream.readObject();
            messageDistribution = messageCounter.getValueDistribution();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try {
            File outputFile = new FileHandler(outputFolder, "message_count_distribution", ".obj").nextFile();
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                outputStream.writeObject(messageDistribution);
                outputStream.flush();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        SamplingDiscreteTreeDistribution<LocalTime> timeDistribution;
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(args[3]))) {
            TimeCounter timeCounter = (TimeCounter) objectInputStream.readObject();
            timeDistribution = timeCounter.getValueDistribution();
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try {
            File outputFile = new FileHandler(outputFolder, "time_count_distribution", ".obj").nextFile();
            try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
                outputStream.writeObject(timeDistribution);
                outputStream.flush();
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
