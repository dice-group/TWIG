package org.aksw.twig.wordembeddings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

// http://0.0.0.0:4441/word2vec/vector?a=cat&apikey=1234
public class WordEmbeddings {
  private static final Logger LOGGER = LogManager.getLogger(WordEmbeddings.class);

  final String apiKey = "1234";
  final String url = "http://0.0.0.0:4441/word2vec/vector?apikey=%s&a=%s";

  // replace me with WordMatrix
  public final Map<Integer, String> index = new HashMap<>();
  public final Map<String, Integer> indexReverse = new HashMap<>();
  // replace me with WordMatrix

  // TODO: we do not need a map here.
  public Map<Integer, double[]> wordVecMap = new HashMap<>();

  // words without vectors
  List<String> blackList = new ArrayList<>();

  public int addToIndex(final String word) {
    if (indexReverse.get(word) == null) {
      final int id = index.size();
      index.put(id, word);
      indexReverse.put(word, id);
    }
    return indexReverse.get(word);
  }

  /**
   * Replace me with deeplearning4j
   *
   * @param word
   * @return vector or null
   */
  public double[] requestWord2Vec(final String word) {

    if ((wordVecMap.get(word) == null) && !blackList.contains(word)) {

      String response = "";
      try {
        response = Requests.get(String.format(url, apiKey, word));
      } catch (final ClientProtocolException e) {
        LOGGER.error(e.getLocalizedMessage());
      } catch (final IOException e) {
        LOGGER.error(e.getLocalizedMessage());
      }

      final JSONObject o = new JSONObject(response);
      if (o.has("a") && o.getJSONObject("a").has("vec")) {
        final JSONArray ja = o.getJSONObject("a").getJSONArray("vec");
        final double[] vec = new double[ja.length()];
        for (int i = 0; i < ja.length(); i++) {
          vec[i] = ja.getDouble(i);
        }
        addToIndex(word);
        wordVecMap.put(indexReverse.get(word), vec);
      } else {
        blackList.add(word);
      }
    }
    return wordVecMap.get(word);
  }

  /**
   *
   * @param word
   * @param vec
   */
  public void addToMap(final String word, final double[] vec) {
    wordVecMap.put(indexReverse.get(word), vec);
  }

  public static void main(final String[] a) {

    /**
     * <code>
     final File file =
         new File("/media/rspeck/store/GitRepos/TWIG/data/GoogleNews-vectors-negative300.bin.gz");
     final Word2Vec word2Vec = WordVectorSerializer.readWord2VecModel(file);

     LOGGER.info(word2Vec.getStopWords());

     final double[] wordvec = word2Vec.getWordVector("cat");

     for (final double d : wordvec) {
       LOGGER.info(d);
     }
    </code>
     */

  }
}
