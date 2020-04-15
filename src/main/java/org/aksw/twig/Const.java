package org.aksw.twig;

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
  public static long SEED;

  // number of statements in model
  public static int MODEL_MAX_SIZE;

  // WordSampler
  public static double DISTRIBUTION_CHANCE_DELTA;
  // WordSampler
  public static double TRUNCATE_CHANCE;

  // LoadModels
  public static double DEFAULT_TRUNCATE_TO;
  // LoadModels
  public static long DEFAULT_MAX_MEMORY;
  // LoadModels
  public static double MEMORY_USE;
  // LoadModels

  /**
   * Loads the config file and inits the constants.
   */
  public static void load(final String configFile) {
    try {
      final List<String> lines;
      lines = Files.readLines(Paths.get(configFile).toFile(), StandardCharsets.UTF_8);
      final JSONObject o = new JSONObject(String.join("", lines));

      N_THREADS_TWITTER7PARSER = o.getInt("twitterBlockThreads");
      N_THREADS_TWITTER7PARSER_MAIN = o.getInt("twitter7ParserThreads");
      N_THREADS_SELFSUSPENDINGEXECUTOR = o.getInt("selfsuspendingexecutor");
      SEED = o.getInt("seed");
      MODEL_MAX_SIZE = o.getInt("modelSize");
      DISTRIBUTION_CHANCE_DELTA = o.getDouble("delta");
      TRUNCATE_CHANCE = o.getDouble("truncateChance");

      DEFAULT_TRUNCATE_TO = o.getDouble("defaultTruncateTo");
      DEFAULT_MAX_MEMORY = o.getLong("defaultMaxMemory");
      MEMORY_USE = o.getDouble("memoryUse");
    } catch (final Exception e) {
      LOG.error(e.getLocalizedMessage());
    }
  }
}
