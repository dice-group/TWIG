package org.aksw.twig.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.Set;

import org.aksw.twig.automaton.data.SamplingWordPredecessorSuccessorDistribution;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.files.FileHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Samples {

  private static final Logger LOG = LogManager.getLogger(Samples.class);

  public static void main(final String[] args) {
    final Pair<File, Set<File>> parsedArgs = FileHandler.readArgs(args);
    // read parameters
    final String out = parsedArgs.getLeft().getAbsolutePath().toString();
    final int seed = Integer.parseInt(args[1]);
    final int j = Integer.parseInt(args[2]);
    final String infile = args[3];

    final SamplingWordPredecessorSuccessorDistribution s;
    s = new WordSampler(new LoadModels().loadWordMatrix(infile));

    new Random(seed);
    s.reseedRandomGenerator(seed);

    final StringBuffer sb = new StringBuffer();
    for (int i = 0; i < j; i++) {
      sb.append(s.sample());
      sb.append(System.lineSeparator());
    }

    final File outputDirectory = Paths.get(out).toFile();
    final String outputFile = Paths.get(outputDirectory.getAbsolutePath()
        .concat(outputDirectory.getAbsolutePath().endsWith("/") ? "" : "/").concat("samples.txt"))
        .toFile().getAbsolutePath();

    final File file = new File(outputFile);
    try {
      Files.write(sb.toString(), file, Charsets.UTF_8);
    } catch (final IOException e) {
      LOG.error(e.getMessage(), e);
    }
  }
}
