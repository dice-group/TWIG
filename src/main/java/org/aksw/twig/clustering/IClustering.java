package org.aksw.twig.clustering;

import java.util.Set;

/**
 *
 * @author Ren&eacute; Speck <speck@informatik.uni-leipzig.de>
 *
 */
public interface IClustering {

  void add(Set<String> words);

  void cluster();

  // public Map<Integer, Set<String>> getClusters();

  String getRandomWordFromCluster(String word);
}
