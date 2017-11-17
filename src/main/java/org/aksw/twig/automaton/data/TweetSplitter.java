package org.aksw.twig.automaton.data;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

/**
 * This class splits tweets in pairs of words succeeding each other. A pair of predecessor and
 * successor is determined by following regex:<br/>
 * {@code ([a-zA-Z0-9'@#]+)[ ,-]+([a-zA-Z0-9'@#]+)}<br/>
 * <br/>
 *
 * The first group is succeeded by the second. Words can only succeed one another if they are in the
 * same sentence. Sentences are delimited by '.', '!' or '?'.<br/>
 * If the empty string is succeeded by a word that means the word starts the sentence. If a word is
 * succeeded by the empty string that means the word ends the sentence.
 */
class TweetSplitter implements Iterable<Pair<String, String>> {

  private static final String SENTENCE_DELIMITING_REGEX = "[!?.]+";

  private static final String WORD_DELIMITING_REGEX = "[ -,]+";

  private static final String UNWANTED_SEQUENCES = "[^a-zA-Z0-9#'@ -,?!.]+";

  /**
   * Creates a new instance and splits given tweet.
   * 
   * @param tweet Tweet to split.
   */
  public TweetSplitter(String tweet) {
    splitTweet(tweet.replaceAll(UNWANTED_SEQUENCES, ""));
  }

  private final List<Pair<String, String>> split = new LinkedList<>();

  /**
   * Returns a stream to all pairs of predecessors and successors.
   * 
   * @return Stream to pairs of predecessors and successors. If no tweet was split the stream will
   *         be empty.
   */
  public Stream<Pair<String, String>> getSplit() {
    return this.split.stream();
  }

  @Override
  public Iterator<Pair<String, String>> iterator() {
    return this.split.iterator();
  }

  /**
   * Splits all words in the tweet.
   * 
   * @param tweet Tweet to split words in.
   */
  private void splitTweet(String tweet) {
    String[] sentences = tweet.split(SENTENCE_DELIMITING_REGEX);
    for (String sentence : sentences) {
      sentence = sentence.trim();
      String[] words = sentence.split(WORD_DELIMITING_REGEX);

      String lastWord = "";
      for (String word : words) {
        if (word.equals("")) {
          continue;
        }

        split.add(new ImmutablePair<>(lastWord, word));
        lastWord = word;
      }

      if (!lastWord.equals("")) {
        split.add(new ImmutablePair<>(lastWord, ""));
      }
    }
  }
}
