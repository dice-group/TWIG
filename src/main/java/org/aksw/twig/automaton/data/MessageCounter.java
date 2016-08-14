package org.aksw.twig.automaton.data;

import org.aksw.twig.model.Twitter7ModelWrapper;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SimpleExponentialRegression;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Counts messages sent by users. Each users will be mapped to their message count.
 * This frequency distribution can be transformed into a {@link ExponentialLikeDistribution} probability measure.
 */
public class MessageCounter implements Serializable {

    private static final long serialVersionUID = 5741136390921853596L;

    private static transient final Logger LOGGER = LogManager.getLogger(MessageCounter.class);

    private Map<String, Integer> userMessageCountMap = new HashMap<>();

    /**
     * Returns all users mapped to their number of messages.
     * @return Stream to user->message count map.
     */
    public Stream<Map.Entry<String, Integer>> getUserMessageCountMap() {
        return userMessageCountMap.entrySet().stream();
    }

    private ArrayList<Integer> messageCounts;

    /**
     * Returns an array list with following semantics:
     * {@code x := arrayList[i]} means that {@code x} users have sent {@code i + 1} messages.
     * @return Array list with message numbers.
     */
    public ArrayList<Integer> getMessageCounts() {
        if (messageCounts == null) {
            messageCounts = new ArrayList<>();
            userMessageCountMap.values().forEach(count -> {
                while (messageCounts.size() <= count - 1) {
                    messageCounts.add(0);
                }
                messageCounts.set(count - 1, messageCounts.get(count - 1) + 1);
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
     * Merges the message counts of given {@link MessageCounter} into this.
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
}
