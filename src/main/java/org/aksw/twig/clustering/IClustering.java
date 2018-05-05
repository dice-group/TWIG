package org.aksw.twig.clustering;

import java.util.Set;

/**
 *
 * @author Ren&eacute; Speck <speck@informatik.uni-leipzig.de>
 *
 */
public interface IClustering {

  public void add(Set<String> words);

  public void cluster();

  // public Map<Integer, Set<String>> getClusters();

  public String getRandomWordFromCluster(String word);
}
