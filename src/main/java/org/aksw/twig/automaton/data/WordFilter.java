package org.aksw.twig.automaton.data;

import java.util.Arrays;

public class WordFilter {
  /**
   *
   * @param word
   * @return
   */
  public boolean isPredecessor(final String word) {
    if (word.startsWith("@")) {
      return false;
    }

    if (Arrays.asList("RT", ".", "!", "?", ",").contains(word)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @param word
   * @return
   */
  public boolean isSuccessor(final String word) {
    if (word.startsWith("@")) {
      return false;
    }

    if (Arrays.asList("RT").contains(word)) {
      return false;
    }
    return true;
  }

  /**
   *
   * @param word
   * @return
   */
  public boolean needsSpace(final String word) {
    if (Arrays.asList(":", ".", "!", "?", ",").contains(word)) {
      return false;
    }
    return true;
  }
}
