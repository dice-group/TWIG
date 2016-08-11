package org.aksw.twig.automaton.messageCount;

import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.Twitter7ModelWrapper;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SimpleExponentialRegression;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Counts messages per user. Intended to be used on a Twitter7Model as it can be created by {@link Twitter7ModelWrapper}.
 */
public class MessageCounter implements Serializable {

    private static final long serialVersionUID = 5741136390921853596L;

    private static final Logger LOGGER = LogManager.getLogger(MessageCounter.class);

    private Map<String, Integer> userMessageCountMap = new HashMap<>();

    /**
     * Returns all users mapped to their number of messages.
     * @return Stream to user - message count map.
     */
    public Stream<Map.Entry<String, Integer>> getUserMessageCountMap() {
        return userMessageCountMap.entrySet().stream();
    }

    private ArrayList<Integer> messageCounts;

    /**
     * Returns an array list with following semantics:
     * {@code x := arrayList[i]} means that {@code x} users have sent {@code i} messages.
     * @return Array list with message numbers.
     */
    public ArrayList<Integer> getMessageCounts() {
        if (messageCounts == null) {
            messageCounts = new ArrayList<>();
            userMessageCountMap.values().forEach(count -> {
                while (messageCounts.size() <= count) {
                    messageCounts.add(0);
                }
                messageCounts.set(count, messageCounts.get(count) + 1);
            });
        }

        return messageCounts;
    }

    /**
     * Adds all messages in the model to the counter.
     * @param model Model to add.
     */
    public void addModel(Model model) {
        model.listStatements().forEachRemaining(statement -> {
            if (statement.getPredicate().getLocalName().equals(Twitter7ModelWrapper.SENDS_PROPERTY_NAME)) {
                String userName = statement.getSubject().getLocalName();
                addUserMessages(userName, 1);
            }
        });
    }

    /**
     * Adds given number of messages to the user.
     * @param userName User to add to.
     * @param messages Message number to add.
     */
    public void addUserMessages(String userName, int messages) {
        messageCounts = null;

        if (!userMessageCountMap.containsKey(userName)) {
            userMessageCountMap.put(userName, messages);
        } else {
            userMessageCountMap.put(userName, userMessageCountMap.get(userName) + messages);
        }
    }

    /**
     * Creates an exponential like distribution over values of {@link #getMessageCounts()}.
     * @return Distribution.
     */
    public ExponentialLikeDistribution getValueDistribution() {
        getMessageCounts(); // Init array list if necessary
        SimpleExponentialRegression regression = new SimpleExponentialRegression();
        for (int i = 0; i < messageCounts.size(); i++) {
            regression.addData(i, messageCounts.get(i));
        }

        return ExponentialLikeDistribution.of(regression);
    }

    /**
     * Merges given {@link MessageCounter} into this.
     * @param counter {@link MessageCounter} to merge.
     * @return {@code this}
     */
    public void merge(MessageCounter counter) {
        counter.getUserMessageCountMap().forEach(entry -> {
            String userName = entry.getKey();
            Integer messageCount = entry.getValue();
            if (userMessageCountMap.containsKey(userName)) {
                userMessageCountMap.put(userName, userMessageCountMap.get(userName) + messageCount);
            } else {
                userMessageCountMap.put(userName, messageCount);
            }
        });
    }

    /**
     * Logs the content of the array list as returned by {@link #getMessageCounts()}.
     */
    public void logResults() {
        getMessageCounts();
        for (int i = 0; i < messageCounts.size(); i++) {
            LOGGER.info("{} users have sent between {} messages.", messageCounts.get(i), i);
        }
    }

    /**
     * Creates a message counter distribution as stated in {@link #getValueDistribution()} and the messages counts as stated in {@link #getMessageCounts()} by adding all given files as {@link Model} and writes it into a file.
     * Arguments must be formatted as stated in {@link FileHandler#readArgs(String[])}.
     * @param args Arguments.
     */
    public static void main(String[] args) {

        // File handling
        Pair<File, Set<File>> fileArgs = FileHandler.readArgs(args);

        if (fileArgs.getLeft() == null) {
            throw new IllegalArgumentException("No --out argument given.");
        }

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

        messageCounter.logResults();

        if (fileArgs.getLeft() == null) {
            return;
        }

        File outputFile;
        try {
            outputFile = new FileHandler(fileArgs.getLeft(), "message_count", ".obj").nextFile();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            objectOutputStream.writeObject(messageCounter);
            objectOutputStream.flush();
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
