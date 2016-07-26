package org.aksw.twig.automaton.tweets;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

/**
 * This class splits tweets in pairs of words succeeding each other. A pair of predecessor and successor is determined by following regex:<br/>
 * {@code ([a-zA-Z0-9'@#]+)[ ,-]+([a-zA-Z0-9'@#]+)}<br/><br/>
 *
 * The first group is succeeded by the second. Words can only succeed one another if they are in the same sentence. Sentences are delimited by '.', '!' or '?'.<br/>
 * If the empty string is succeeded by a word that means the word starts the sentence.
 * If a word is succeeded by the empty string that means the word ends the sentence.
 */
class TweetSplitter implements Iterable<Pair<String, String>> {

    private static final Set<Character> SENTENCE_DELIMITERS = new HashSet<>();

    private static final Set<Character> WORD_DELIMITERS = new HashSet<>();

    private static final String ALLOWED_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'@#";

    private static final Set<Character> ALLOW_CHARACTERS = new HashSet<>();

    static
    {
        SENTENCE_DELIMITERS.add('.');
        SENTENCE_DELIMITERS.add('!');
        SENTENCE_DELIMITERS.add('?');
        WORD_DELIMITERS.add(' ');
        WORD_DELIMITERS.add(',');
        WORD_DELIMITERS.add('-');
        ALLOW_CHARACTERS.addAll(SENTENCE_DELIMITERS);
        ALLOW_CHARACTERS.addAll(WORD_DELIMITERS);
        for (int i = 0; i < ALLOWED_CHARACTERS.length(); i++) {
            ALLOW_CHARACTERS.add(ALLOWED_CHARACTERS.charAt(i));
        }
    }

    /**
     * Basic constructor with no further functionality.
     */
    public TweetSplitter() {}

    /**
     * Creates a new instance and splits given tweet.
     * @param tweet Tweet to split.
     */
    public TweetSplitter(String tweet) {
        splitTweet(tweet);
    }

    private List<Pair<String, String>> split;

    /**
     * Returns a stream to all pairs of predecessors and successors.
     * @return Stream to pairs of predecessors and successors. If no tweet was split the stream will be empty.
     */
    public Stream<Pair<String, String>> getSplit() {
        return this.split == null ? Stream.empty() : this.split.stream();
    }

    @Override
    public Iterator<Pair<String, String>> iterator() {
        return this.split.iterator();
    }

    /**
     * Splits all words in the tweet.
     * @param tweet Tweet to split words in.
     * @return Stream to split words.
     */
    public Stream<Pair<String, String>> splitTweet(String tweet) {
        this.split = new LinkedList<>();
        boolean lastCharDelimited = false;
        String predecessor = "";
        StringBuilder successor = new StringBuilder();
        for (int i = 0; i < tweet.length(); i++) {
            char c = tweet.charAt(i);

            if (!ALLOW_CHARACTERS.contains(c)) {
                continue;
            }

            if (WORD_DELIMITERS.contains(c)) {
                if (!lastCharDelimited) {
                    this.split.add(new ImmutablePair<>(predecessor, successor.toString()));
                    predecessor = successor.toString();
                    successor = new StringBuilder();
                }
                lastCharDelimited = true;
                continue;
            }

            if (SENTENCE_DELIMITERS.contains(c)) {
                if (!lastCharDelimited) {
                    this.split.add(new ImmutablePair<>(predecessor, successor.toString()));
                    this.split.add(new ImmutablePair<>(successor.toString(), ""));
                    predecessor = "";
                    successor = new StringBuilder();
                }
                lastCharDelimited = true;
                continue;
            }

            lastCharDelimited = false;
            successor.append(c);
        }

        return getSplit();
    }
}
