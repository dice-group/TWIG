package org.aksw.twig.automaton.learning;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;

public class TweetSplitterTest {

    @Test
    public void splitTweet() {
        TweetSplitter splitter = new TweetSplitter("@Audition_Portal PAAAUULLLL! I miss youuu! Lol I thought I sent you an email, but it was placed in my Drafts so I'll resend that to you! =]");
        Set<Pair<String, String>> tweet = splitter.getSplit().collect(Collectors.toSet());
        // Check randoms
        Assert.assertTrue(tweet.contains(new ImmutablePair<>("miss", "youuu")));
        Assert.assertTrue(tweet.contains(new ImmutablePair<>("Drafts", "so")));
        // Check sentence start
        Assert.assertTrue(tweet.contains(new ImmutablePair<>("", "I")));
        // Check sentence end
        Assert.assertTrue(tweet.contains(new ImmutablePair<>("you", "")));
        // Check punctuation
        Assert.assertTrue(tweet.contains(new ImmutablePair<>("email", "but")));
    }
}
