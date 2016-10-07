package org.aksw.twig.automaton.data;

import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.ExponentialLikeDistribution;
import org.aksw.twig.statistics.SimpleExponentialRegression;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Counts messages sent by users. Each users will be mapped to their message count.
 * This frequency distribution can be transformed into a {@link ExponentialLikeDistribution} probability measure.
 */
public class MessageCounter implements Serializable {

    private Map<String, Integer> userMessageCountMap = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(MessageCounter.class);

    private static final long serialVersionUID = 5741136390921853596L;

    private Map<String, Integer> userMessageDayIntervalMap = new HashMap<>();

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
                if (count != 0) {
                    while (messageCounts.size() <= count - 1) {
                        messageCounts.add(0);
                    }
                    messageCounts.set(count - 1, messageCounts.get(count - 1) + 1);
                }
            });
        }

        return messageCounts;
    }

    /**
     * Adds all messages in the model to the counter. TODO
     * @param model Model to add.
     */
    public void addModel(Model model) {

        Map<String, Set<String>> userToMessagesMapping = new HashMap<>();
        Map<String, LocalDate> messageToDateMapping = new HashMap<>();

        model.listStatements().forEachRemaining(statement -> {

            if (statement.getPredicate().getLocalName().equals(TWIGModelWrapper.SENDS_PROPERTY_NAME)) {
                String userName = statement.getSubject().getLocalName();
                String tweetId = statement.getObject().asResource().getLocalName();
                userToMessagesMapping.computeIfAbsent(userName, x -> new HashSet<>()).add(tweetId);

            } else if (statement.getPredicate().getLocalName().equals(TWIGModelWrapper.TWEET_TIME_PROPERTY_NAME)) {
                String tweetId = statement.getSubject().getLocalName();
                LocalDate tweetDate = LocalDate.from(TWIGModelWrapper.DATE_TIME_FORMATTER.parse(statement.getObject().asLiteral().getString()));
                messageToDateMapping.put(tweetId, tweetDate);
            }
        });

        userToMessagesMapping.entrySet().forEach(entry -> {
            String userName = entry.getKey();
            Set<String> tweets = entry.getValue();
            setUserMessages(userName, tweets.size());

            Set<LocalDate> dates = tweets.stream()
                    .map(messageToDateMapping::get)
                    .collect(Collectors.toSet());

            LocalDate lowest = dates.stream()
                    .min(LocalDate::compareTo)
                    .orElse(null);

            LocalDate highest = dates.stream()
                    .max(LocalDate::compareTo)
                    .orElse(null);

            setUserDayInterval(userName, (int) ChronoUnit.DAYS.between(lowest, highest) + 1);
        });
    }

    /**
     * Adds given number of messages to the user.
     * @param userName User to add to.
     * @param messages Message number to add.
     */
    public void setUserMessages(String userName, int messages) {
        messageCounts = null;
        userMessageCountMap.put(userName, messages);
    }

    /**
     * Returns how many messages have been sent by the user.
     * @param userName User to check.
     * @return Message count of the user.
     */
    public int getUserMessages(String userName) {
        return userMessageCountMap.getOrDefault(userName, -1);
    }

    /**
     * Sets the amount of days by which the user has sent his tweets.
     * @param userName User to set.
     * @param dayInterval Amount of days.
     */
    public void setUserDayInterval(String userName, int dayInterval) {
        userMessageDayIntervalMap.put(userName, dayInterval);
    }

    /**
     * Returns a new message counter which has all users normalized to given time period. Each user will have it's message count modified by:<br>
     * {@code count = count / userDayInterval * normalPeriod.toDays();}<br><br>
     * A code example:
     * <pre>
     * {@code MessageCounter counter = new MessageCounter();}
     * {@code counter.setUserMessageCount("user", 10);}
     * {@code counter.setUserDayInterval("user", 5);}
     * {@code counter = counter.normalized(Duration.ofDays(1);}
     * {@code counter.getUserMessages("user"); // will return 2}
     * </pre>
     * @param normalPeriod Time period to normalize to.
     * @return Normalized message counter.
     */
    public MessageCounter normalized(Duration normalPeriod) {

        MessageCounter messageCounter = new MessageCounter();

        userMessageDayIntervalMap.entrySet().forEach(dayIntervalEntry -> {
            String userName = dayIntervalEntry.getKey();
            double factor = (double) normalPeriod.toDays() / (double) dayIntervalEntry.getValue();

            int newMessageCount = (int) Math.round(((double) userMessageCountMap.get(userName) * factor));
            if (newMessageCount > 0) {
                messageCounter.setUserMessages(userName, newMessageCount);
                messageCounter.setUserDayInterval(userName, (int) normalPeriod.toDays());
            } else {
                LOGGER.info("User {} has 0 messages after normalization.", userName);
            }
        });

        return messageCounter;
    }

    /**
     * Creates an exponential like distribution over values of {@link #getMessageCounts()}. There must by some messages counted in order for this method to work.
     * Otherwise an {@link IllegalStateException} is thrown.
     * @return Distribution.
     */
    public ExponentialLikeDistribution getValueDistribution() {
        if (userMessageCountMap.isEmpty()) {
            throw new IllegalStateException("Cannot create a value distribution of an empty MessageCounter.");
        }

        getMessageCounts(); // Init array list if necessary
        SimpleExponentialRegression regression = new SimpleExponentialRegression();
        for (int i = 0; i < messageCounts.size(); i++) {
            if (messageCounts.get(i) == 0.0) {
                continue;
            }

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
