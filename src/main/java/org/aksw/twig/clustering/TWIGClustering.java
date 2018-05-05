package org.aksw.twig.clustering;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.aksw.twig.wordembeddings.WordEmbeddings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.lmu.ifi.dbs.elki.algorithm.clustering.DBSCAN;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.KMeansLloyd;
import de.lmu.ifi.dbs.elki.algorithm.clustering.kmeans.initialization.RandomlyGeneratedInitialMeans;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.model.KMeansModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRange;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.ArrayAdapterDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.CosineDistanceFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;

// TODO: use k-means or k-medoids not DBSCAN
// examples:
// https://github.com/elki-project/elki/blob/master/addons/tutorial/src/main/java/tutorial/javaapi/PassingDataToELKI.java
public class TWIGClustering implements IClustering {

  private static final Logger LOGGER = LogManager.getLogger(TWIGClustering.class);

  protected WordEmbeddings wordEmbeddings = new WordEmbeddings();

  // maps the word index id to its clusters
  Map<Integer, Integer> wordToCluster = new HashMap<>();
  int[][] clusters = null;
  Random rand = new Random();

  /**
   *
   * Constructor.
   *
   */
  public TWIGClustering() {

    // nextInt as provided by Random is exclusive of the top value so you need to add 1

  }

  @Override
  public void add(final Set<String> words) {
    LOGGER.debug("add ...");

    words.forEach(wordEmbeddings::requestWord2Vec);
  }

  @Override
  public void cluster() {
    LOGGER.debug("cluster ...");

    // create data
    double[][] data;
    data = new double[wordEmbeddings.wordVecMap
        .size()][wordEmbeddings.wordVecMap.values().iterator().next().length];

    for (final Entry<Integer, double[]> entry : wordEmbeddings.wordVecMap.entrySet()) {
      data[entry.getKey().intValue()] = entry.getValue();
    }
    // start clustering
    cluster(data);
  }

  @Override
  public String getRandomWordFromCluster(final String word) {
    // LOGGER.debug("getRandomWordFromCluster ...");

    String randomWord = null;
    if (clusters != null) {

      if (wordEmbeddings.indexReverse.get(word) == null) {
        return null;
      }

      // index ot the word
      final int indexId = wordEmbeddings.indexReverse.get(word);

      // id of the cluster with the word
      final int clusterId = wordToCluster.get(indexId);

      // the cluster elements
      final int[] clusterIds = clusters[clusterId];

      // get a random element
      final int min = 0;
      final int max = clusterIds.length - 1;
      final int randomNum = rand.nextInt((max - min) + 1) + min;
      final int randomId = clusterIds[randomNum];
      randomWord = wordEmbeddings.index.get(randomId);
    } else {
      LOGGER.warn("Clusters not set yet!");
    }
    return randomWord;
  }

  /**
   *
   * @param data
   * @return
   */
  protected int[][] cluster(final double[][] data) {
    final DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
    // Create a database (which may contain multiple relations!)
    final Database db = new StaticArrayDatabase(dbc, null);
    // Load the data into the database (do NOT forget to initialize...)
    db.initialize();
    // Relation containing the number vectors:
    final Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    // We know that the ids must be a continuous range:
    final DBIDRange ids = (DBIDRange) rel.getDBIDs();

    // K-means should be used with squared Euclidean (least squares):
    final CosineDistanceFunction dist = CosineDistanceFunction.STATIC;
    // Default initialization, using global random:
    // To fix the random seed, use: new RandomFactory(seed);
    final RandomlyGeneratedInitialMeans init =
        new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);

    // Textbook k-means clustering:
    final KMeansLloyd<NumberVector> km = new KMeansLloyd<>(dist, //
        3 /* k - number of partitions */, //
        0 /* maximum number of iterations: no limit */, init);

    // K-means will automatically choose a numerical relation from the data set:
    // But we could make it explicit (if there were more than one numeric
    // relation!): km.run(db, rel);
    final Clustering<KMeansModel> c = km.run(db);

    // Output all clusters:
    int i = 0;

    final List<Cluster<KMeansModel>> clustersOriginal = c.getAllClusters();

    clusters = new int[clustersOriginal.size()][];

    for (final Cluster<KMeansModel> clu : clustersOriginal) {
      final int[] clusterElements = new int[clu.size()];
      if (!clu.isNoise()) {
        int j = 0;
        for (final DBIDIter it = clu.getIDs().iter(); it.valid(); it.advance()) {
          final int offset = ids.getOffset(it);
          // System.out.print(" " + offset);
          clusterElements[j++] = offset;
          if (wordToCluster.get(offset) != null) {
            LOGGER.warn("Should not happen!");
          } else {
            wordToCluster.put(offset, i);
          }
        } // end for
      } // end if
      clusters[i] = clusterElements;
      ++i;
    } // end for
    return clusters;
  }

  protected int[][] _cluster(final double[][] data) {

    // Adapter to load data from an existing array.
    final DatabaseConnection dbc = new ArrayAdapterDatabaseConnection(data);
    // Create a database (which may contain multiple relations!)
    final Database db = new StaticArrayDatabase(dbc, null);
    // Load the data into the database (do NOT forget to initialize...)
    db.initialize();

    // Relation containing the number vectors:
    final Relation<NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    // We know that the ids must be a continuous range:
    final DBIDRange ids = (DBIDRange) rel.getDBIDs();

    final CosineDistanceFunction dist = CosineDistanceFunction.STATIC;

    // new RandomlyGeneratedInitialMeans(RandomFactory.DEFAULT);

    // Textbook k-means clustering:
    final double epsilon = 0.5D;
    final int minpts = 2;
    final DBSCAN<NumberVector> km = new DBSCAN<NumberVector>(dist, epsilon, minpts);
    // new DBSCAN<>(dist, //
    // 2 /* k - number of partitions */, //
    // 0 /* maximum number of iterations: no limit */, init);

    // K-means will automatically choose a numerical relation from the data set:
    // But we could make it explicit (if there were more than one numeric
    // relation!): km.run(db, rel);
    final Clustering<Model> c = km.run(db);

    int i = 0;
    for (final Cluster<Model> clusters : c.getAllClusters()) {

      if (!clusters.isNoise()) {

        System.out.println("#" + i + ": " + clusters.getNameAutomatic());
        System.out.println("Size: " + clusters.size());

        // TODO: get contorid

        // System.out.println("Center: " + clusters.getModel().getPrototype().toString());
        // Iterate over objects:
        System.out.print("Objects: ");

        for (final DBIDIter it = clusters.getIDs().iter(); it.valid(); it.advance()) {
          // To get the vector use:
          // NumberVector v = rel.get(it);

          // Offset within our DBID range: "line number"
          final int offset = ids.getOffset(it);
          System.out.print(" " + offset);
          // Do NOT rely on using "internalGetIndex()" directly!
        }

        System.out.println();
        ++i;
      }
    }
    return null;
  }
}
