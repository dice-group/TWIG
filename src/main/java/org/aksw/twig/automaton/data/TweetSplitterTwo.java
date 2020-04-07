package org.aksw.twig.automaton.data;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cmu.arktweetnlp.Twokenize;

/**
 */
class TweetSplitterTwo implements Iterable<Pair<String, String>> {

  private static final Logger LOG = LogManager.getLogger(TweetSplitterTwo.class);

  /**
   * Creates a new instance and splits given tweet.
   *
   * @param tweet Tweet to split.
   */
  public TweetSplitterTwo(final String tweet) {
    splitTweet(tweet);
  }

  private final List<Pair<String, String>> split = new LinkedList<>();

  /**
   * Returns a stream to all pairs of predecessors and successors.
   *
   * @return Stream to pairs of predecessors and successors. If no tweet was split the stream will
   *         be empty.
   */
  public Stream<Pair<String, String>> getSplit() {
    return split.stream();
  }

  @Override
  public Iterator<Pair<String, String>> iterator() {
    return split.iterator();
  }

  /**
   * Splits all words in the tweet.
   *
   * @param tweet Tweet to split words in.
   */
  private void splitTweet(final String tweet) {
    try {
      String lastWord = "";
      for (final String word : Twokenize.tokenizeRawTweetText(tweet)) {
        if (word.equals("")) {
          continue;
        }
        split.add(new ImmutablePair<>(lastWord, word));
        lastWord = word;
      }
      if (!lastWord.equals("")) {
        split.add(new ImmutablePair<>(lastWord, ""));
      }
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage(), e);
    }
  }
}
