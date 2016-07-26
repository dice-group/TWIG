package org.aksw.twig.automaton.messageCount;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TwitterModelWrapper;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SimpleExponentialRegression;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Set;

public class MessageCounter {

    private static final Logger LOGGER = LogManager.getLogger(MessageCounter.class);

    private static final Query USER_MESSAGE_COUNT_QUERY = QueryFactory.create(
            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
            "PREFIX twig: <http://aksw.org/twig#> " +
            "SELECT ?user (COUNT(?msg) AS ?count) WHERE { " +
                    "?user rdf:type twig:OnlineTwitterAccount . " +
                    "?msg rdf:type twig:Tweet . " +
                    "?user twig:sends ?msg . " +
            "} GROUP BY ?user"
    );

    private static final int MESSAGE_STEP_SIZE = 100;

    private ArrayList<Integer> messageCounts = new ArrayList<>();

    void addModel(Model model) {
        try (QueryExecution execution = QueryExecutionFactory.create(USER_MESSAGE_COUNT_QUERY, model)) {
            ResultSet resultSet = execution.execSelect();
            resultSet.forEachRemaining(querySolution -> {
                int messageCount = querySolution.getLiteral("count").getInt();
                messageCount /= 100;
                while (messageCounts.size() < messageCount + 1) {
                    messageCounts.add(0);
                }
                messageCounts.set(messageCount, messageCounts.get(messageCount) + 1);
            });
        }
    }

    ExponentialLikeDistribution getValueDistribution() {
        for (int i = 0; i < messageCounts.size(); i++) {
            LOGGER.info("{} - {}", i * MESSAGE_STEP_SIZE, messageCounts.get(i));
        }

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
            LOGGER.error(e.getMessage(), e);
            return;
        }

        // Message counting
        MessageCounter messageCounter = new MessageCounter();
        fileArgs.getRight().forEach(file -> {
            try {
                TwitterModelWrapper modelWrapper = TwitterModelWrapper.read(file);
                messageCounter.addModel(modelWrapper.getModel());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(messageCounter.getValueDistribution());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
