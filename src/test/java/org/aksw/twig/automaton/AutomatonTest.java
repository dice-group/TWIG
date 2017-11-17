package org.aksw.twig.automaton;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import org.aksw.twig.Main;
import org.aksw.twig.automaton.data.MessageCounter;
import org.aksw.twig.automaton.data.TimeCounter;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.SamplingDiscreteDistribution;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class AutomatonTest {

  private static final Logger LOGGER = LogManager.getLogger(AutomatonTest.class);

  // @BeforeClass
  public static void before() throws IOException {
    // FileUtils.deleteDirectory(Paths.get("sample/analysis").toFile());
    // FileUtils.deleteDirectory(Paths.get("sample/data").toFile());
    // FileUtils.deleteDirectory(Paths.get("sample/output").toFile());

    FileUtils.forceDelete(Paths.get("sample/analysis").toFile());
    FileUtils.forceDelete(Paths.get("sample/data").toFile());
    FileUtils.forceDelete(Paths.get("sample/output").toFile());

    FileUtils.forceMkdir(Paths.get("sample/analysis").toFile());
    FileUtils.forceMkdir(Paths.get("sample/data").toFile());
    FileUtils.forceMkdir(Paths.get("sample/output").toFile());

    final String[] args = new String[3];
    args[0] = "Twitter7Parser";
    args[1] = "--out=sample/data";
    args[2] = "sample/sample.txt.gz";

    Main.main(args);
  }

  @Test
  public void test() {

  }

  // @Test
  public void tmpTest() {

    MessageCounter messageCounter;
    try (ObjectInputStream inputStream =
        new ObjectInputStream(new FileInputStream("C:/users/felix/desktop/message_count_0.obj"))) {
      messageCounter = (MessageCounter) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      Assert.fail();
      return;
    }

    final SamplingDiscreteDistribution<Integer> distribution =
        messageCounter.getValueDistribution();
    final MessageCounter normalizedMessageCounter = messageCounter.normalize(Duration.ofDays(30));
    final SamplingDiscreteDistribution<Integer> normalizedDistribution =
        normalizedMessageCounter.getValueDistribution();

    for (int i = 0; i < 10; i++) {
      LOGGER.info("{}; normalize: {}", distribution.sample(), normalizedDistribution.sample());
    }
  }

  // @Test
  public void anotherTmpTest() {

    try {
      final TWIGModelWrapper modelWrapper =
          TWIGModelWrapper.read(new File("C:/users/felix/desktop/tweets2009-10_0.ttl.gz"));
      final MessageCounter messageCounter = new MessageCounter();
      final Model model = modelWrapper.getModel();
      messageCounter.addModel(model);
      final MessageCounter normalized = messageCounter.normalize(Duration.ofDays(30));
      final SamplingDiscreteDistribution<Integer> distribution = normalized.getValueDistribution();
      for (int i = 0; i < 10; i++) {
        LOGGER.info(distribution.sample());
      }
    } catch (final IOException e) {
      LOGGER.error(e.getMessage(), e);
      Assert.fail();
    }
  }

  // @Test
  public void AutomatonTest() {

    MessageCounter messageCounter;
    TimeCounter timeCounter;
    WordMatrix wordMatrix;

    try (ObjectInputStream inputStream = new ObjectInputStream(
        new FileInputStream("C:/users/felix/desktop/TWIG_test/message_count_0.obj"))) {
      messageCounter = (MessageCounter) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      Assert.fail();
      return;
    }

    try (ObjectInputStream inputStream = new ObjectInputStream(
        new FileInputStream("C:/users/felix/desktop/TWIG_test/time_count_0.obj"))) {
      timeCounter = (TimeCounter) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      Assert.fail();
      return;
    }

    try (ObjectInputStream inputStream = new ObjectInputStream(
        new FileInputStream("C:/users/felix/desktop/TWIG_test/word_matrix_0.obj"))) {
      wordMatrix = (WordMatrix) inputStream.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      Assert.fail();
      return;
    }

    final SamplingDiscreteDistribution<Integer> messageDistribution =
        messageCounter.getValueDistribution();
    final SamplingDiscreteDistribution<LocalTime> timeDistribution =
        timeCounter.getValueDistribution();
    final WordSampler wordSampler = new WordSampler(wordMatrix);

    final Automaton automaton = new Automaton(wordSampler, messageDistribution, timeDistribution,
        new File("C:/users/felix/desktop/TWIG_test_output"));
    automaton.simulate(50, Duration.ofDays(60), LocalDate.of(2010, 1, 1), 1000);
  }
}
