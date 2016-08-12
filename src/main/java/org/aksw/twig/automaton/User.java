package org.aksw.twig.automaton;

public class User {

    private String name;

    private int messageCount;

    private double messagesPerDay;

    private double messageCounter;

    public User(int messageCount, String name) {
        this.messageCount = messageCount;
        this.name = name;
    }

    public void setDays(long days) {
        messagesPerDay = (double) messageCount / (double) days;
    }

    public int nextDay() {
        messageCounter += messagesPerDay;
        int ret = (int) Math.ceil(messageCounter);
        messageCounter -= ret;
        return ret;
    }

    public String getName() {
        return name;
    }
}
