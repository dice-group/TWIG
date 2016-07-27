package org.aksw.twig.automaton.time;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class TimeCounterTest {

    @Test
    public void test() {
        TimeCounter counter = new TimeCounter();
        LocalDateTime now = LocalDateTime.now().withMinute(0);
        LocalDateTime nowPlusOneSecond = now.withSecond(now.getSecond() + 1);
        LocalDateTime nowPlusOneMinute = now.withMinute(now.getMinute() + 1);
        counter.addTweetTime(now, 2);
        counter.addTweetTime(nowPlusOneSecond, 1);
        counter.addTweetTime(nowPlusOneMinute, 3);

        Assert.assertEquals(3, counter.getTweetTimesAt(now.getHour(), now.getMinute()));
        Assert.assertEquals(3, counter.getTweetTimesAt(nowPlusOneMinute.getHour(), nowPlusOneMinute.getMinute()));
    }
}
