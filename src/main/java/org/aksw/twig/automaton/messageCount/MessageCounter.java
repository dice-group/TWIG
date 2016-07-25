package org.aksw.twig.automaton.messageCount;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TwitterModelWrapper;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SimpleExponentialRegression;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Set;

public class MessageCounter {

    private static final Logger LOGGER = LogManager.getLogger(MessageCounter.class);

    private static final Query USER_MESSAGE_COUNT_QUERY = QueryFactory.create(); // TODO

    private static final int MESSAGE_STEP_SIZE = 100;

    private ArrayList<Integer> messageCounts = new ArrayList<>();

    void addModel(Model model) {
        try (QueryExecution execution = QueryExecutionFactory.create(USER_MESSAGE_COUNT_QUERY, model)) {
            ResultSet resultSet = execution.execSelect();
            // TODO
        }
    }

    ExponentialLikeDistribution getValueDistribution() {
        SimpleExponentialRegression regression = new SimpleExponentialRegression();
        for (int i = 0; i < messageCounts.size(); i++) {
            regression.addData(i, messageCounts.get(i));
        }

        return ExponentialLikeDistribution.of(regression);
    }

    public static void main(String[] args) {

        // File handling
        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);
        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "message_count_distr", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e.getCause());
            return;
        }

        // Message counting
        MessageCounter messageCounter = new MessageCounter();
        fileArgs.getRight().forEach(file -> {
            try (InputStream fileInputStream = FileHandler.getDecompressionStreams(file)) {
                // TODO: is null arg safe?
                Model model = ModelFactory.createDefaultModel().read(fileInputStream, null, TwitterModelWrapper.LANG);
                messageCounter.addModel(model);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e.getCause());
            }
        });

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(messageCounter.getValueDistribution());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e.getCause());
        }
    }
}
