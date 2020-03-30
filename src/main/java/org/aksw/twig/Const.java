package org.aksw.twig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import com.google.common.io.Files;

public class Const {

  private static final Logger LOG = LogManager.getLogger(Const.class);
  static {
    load("config/config.json");
  }

  // how many twitter blocks we use at the same time
  public static int N_THREADS_TWITTER7PARSER;

  // How many Twitter7Parser threads we use, each Twitter7Parser has one file
  public static int N_THREADS_TWITTER7PARSER_MAIN;

  //
  public static int N_THREADS_SELFSUSPENDINGEXECUTOR;

  // seed for random generator
  public static long seed;

  // number of statements in model
  public static int MODEL_MAX_SIZE;

  // WordSampler
  public static double DISTRIBUTION_CHANCE_DELTA;
  // WordSampler
  public static double TRUNCATE_CHANCE;

  /**
   * Loads the config file and inits the constants.
   */
  public static void load(final String configFile) {

    try {
      final List<String> lines =
          Files.readLines(Paths.get(configFile).toFile(), StandardCharsets.UTF_8);
      final String cfg = String.join("", lines);

      final JSONObject o = new JSONObject(cfg);

      N_THREADS_TWITTER7PARSER = o.getInt("twitterBlockThreads");
      N_THREADS_TWITTER7PARSER_MAIN = o.getInt("twitter7ParserThreads");
      N_THREADS_SELFSUSPENDINGEXECUTOR = o.getInt("selfsuspendingexecutor");
      seed = o.getInt("seed");
      MODEL_MAX_SIZE = o.getInt("modelSize");
      DISTRIBUTION_CHANCE_DELTA = o.getDouble("DISTRIBUTION_CHANCE_DELTA");
      TRUNCATE_CHANCE = o.getDouble("TRUNCATE_CHANCE");

    } catch (final IOException e) {
      LOG.error(e.getLocalizedMessage());
    }
  }
}
