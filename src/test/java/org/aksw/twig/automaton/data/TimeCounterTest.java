package org.aksw.twig.automaton.data;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class TimeCounterTest {

    @Test
    public void test() {
        TimeCounter counter = new TimeCounter();
        LocalDateTime testTime = LocalDateTime.of(1995, 2, 12, 12, 0, 0);
        LocalDateTime testTimePlusOneSecond = testTime.withSecond(testTime.getSecond() + 1);
        LocalDateTime testTimePlusOneMinute = testTime.withMinute(testTime.getMinute() + 1);
        counter.addTweetTime(testTime, 2);
        counter.addTweetTime(testTimePlusOneSecond, 1);
        counter.addTweetTime(testTimePlusOneMinute, 3);

        Assert.assertEquals(3, counter.getTimesCountAt(testTime.getHour(), testTime.getMinute()));
        Assert.assertEquals(3, counter.getTimesCountAt(testTimePlusOneMinute.getHour(), testTimePlusOneMinute.getMinute()));
    }
}
