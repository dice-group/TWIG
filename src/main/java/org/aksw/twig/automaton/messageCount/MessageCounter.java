package org.aksw.twig.automaton.messageCount;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SimpleExponentialRegression;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class MessageCounter {

    private static final Logger LOGGER = LogManager.getLogger(MessageCounter.class);

    private static final int MESSAGE_STEP_SIZE = 100;

    private Map<String, Integer> userMessageCountMap = new HashMap<>();

    public Stream<Map.Entry<String, Integer>> getUserMessageCountMap() {
        return userMessageCountMap.entrySet().stream();
    }

    private ArrayList<Integer> messageCounts;

    public ArrayList<Integer> getMessageCounts() {
        if (messageCounts == null) {
            messageCounts = new ArrayList<>();
            userMessageCountMap.values().forEach(count -> {
                count /= MESSAGE_STEP_SIZE;
                while (messageCounts.size() <= count) {
                    messageCounts.add(0);
                }
                messageCounts.set(count, messageCounts.get(count) + 1);
            });
        }

        return messageCounts;
    }

    void addModel(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getPredicate().getLocalName().equals(Twitter7ModelWrapper.SENDS_PROPERTY_NAME)) {
                String userName = statement.getSubject().getLocalName();
                addUserMessages(userName, 1);
            }
        });
    }

    public void addUserMessages(String userName, int messages) {
        messageCounts = null;

        if (!userMessageCountMap.containsKey(userName)) {
            userMessageCountMap.put(userName, messages);
        } else {
            userMessageCountMap.put(userName, userMessageCountMap.get(userName) + messages);
        }
    }

    public ExponentialLikeDistribution getValueDistribution() {
        getMessageCounts(); // Init array list if necessary
        SimpleExponentialRegression regression = new SimpleExponentialRegression();
        for (int i = 0; i < messageCounts.size(); i++) {
            regression.addData(i, messageCounts.get(i));
        }

        return ExponentialLikeDistribution.of(regression);
    }

    public static void main(String[] args) {

        // File handling
        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);

        // Message counting
        MessageCounter messageCounter = new MessageCounter();
        fileArgs.getRight().forEach(file -> {
            try {
                Twitter7ModelWrapper modelWrapper = Twitter7ModelWrapper.read(file);
                messageCounter.addModel(modelWrapper.getModel());
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        });

        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "message_count_distr", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(messageCounter.getValueDistribution());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
