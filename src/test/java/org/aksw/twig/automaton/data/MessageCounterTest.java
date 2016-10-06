package org.aksw.twig.automaton.data;

import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class MessageCounterTest {

    @Test
    public void test() {
        MessageCounter counter = new MessageCounter();
        String userName1 = "a";
        String userName2 = "b";
        counter.setUserMessages(userName1, 2);
        counter.setUserMessages(userName2, 2);

        counter.getUserMessageCountMap().forEach(entry -> {
            String key = entry.getKey();
            if (key.equals(userName1)) {
                Assert.assertEquals(new Integer(2), entry.getValue());
            } else if (key.equals(userName2)) {
                Assert.assertEquals(new Integer(2), entry.getValue());
            } else {
                Assert.fail();
            }
        });

        Assert.assertEquals(new Integer(2), counter.getMessageCounts().get(1));
    }

    @Test
    public void normalizationTest() {
        MessageCounter counter = new MessageCounter();
        counter.setUserMessages("user1", 2);
        counter.setUserMessages("user2", 8);
        counter.setUserDayInterval("user1", 2);
        counter.setUserDayInterval("user2", 8);

        MessageCounter normalized = counter.normalized(Duration.ofDays(4));
        Assert.assertEquals(normalized.getUserMessages("user1"), 4);
        Assert.assertEquals(normalized.getUserMessages("user2"), 4);
    }
}
