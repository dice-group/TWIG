package org.aksw.twig.automaton;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.aksw.twig.Const;
import org.aksw.twig.automaton.data.MessageCounter;
import org.aksw.twig.automaton.data.SamplingWordPredecessorSuccessorDistribution;
import org.aksw.twig.automaton.data.TimeCounter;
import org.aksw.twig.automaton.data.WordMatrix;
import org.aksw.twig.automaton.data.WordSampler;
import org.aksw.twig.files.FileHandler;
import org.aksw.twig.model.TWIGModelWrapper;
import org.aksw.twig.statistics.SamplingDiscreteDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Generates a a TWIG model in a {@link TWIGModelWrapper} by using data analysis results from other
 * TWIG models. The generated TWIG model will include random users and tweets.
 */
public class Automaton {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final int SECONDS = 60;

  public static final int TWEET_NUMBER_NORMALIZATION_DAYS = 30;

  private static final String FILE_NAME = "generated_twig_model";

  private static final String FILE_ENDING = ".ttl";

  private final FileHandler resultStoreFileHandler;

  private final SamplingWordPredecessorSuccessorDistribution samplingWordPredecessorSuccessorDistribution;

  private final SamplingDiscreteDistribution<Integer> tweetNumberDistribution;

  private final SamplingDiscreteDistribution<LocalTime> tweetTimeDistribution;

  /**
   * Creates a new instance setting class variables.
   *
   * @param samplingWordPredecessorSuccessorDistribution Word predecessor-successor distribution
   *        will sample tweets.
   * @param tweetNumberDistribution Tweet number distribution will sample number of tweets per user.
   * @param tweetTimeDistribution Tweet time distribution will sample timestamps of tweets during
   *        the day.
   * @param resultStoreLocation Folder to store resulting models in.
   */
  public Automaton(
      final SamplingWordPredecessorSuccessorDistribution samplingWordPredecessorSuccessorDistribution,
      final SamplingDiscreteDistribution<Integer> tweetNumberDistribution,
      final SamplingDiscreteDistribution<LocalTime> tweetTimeDistribution,
      final File resultStoreLocation) {
    if (!resultStoreLocation.isDirectory()) {
      throw new IllegalArgumentException("resultStoreLocation is no directory");
    }

    resultStoreFileHandler = new FileHandler(resultStoreLocation, FILE_NAME, FILE_ENDING);
    this.samplingWordPredecessorSuccessorDistribution =
        samplingWordPredecessorSuccessorDistribution;
    this.tweetNumberDistribution = tweetNumberDistribution;
    this.tweetTimeDistribution = tweetTimeDistribution;
  }

  /**
   * Generates a TWIG model by using distributions specified in constructor
   * {@link #Automaton(SamplingWordPredecessorSuccessorDistribution, SamplingDiscreteDistribution, SamplingDiscreteDistribution, File)}
   * )}.
   *
   * @param userCount Users to simulate.
   * @param simulationTime Period of time to simulate. Duration will be converted to days.
   * @param startDate Starting date of the simulation period.
   * @param seed Seed for the random number generator.
   * @return TWIG model containing users and tweets.
   */
  public TWIGModelWrapper simulate(final int userCount, final Duration simulationTime,
      final LocalDate startDate, final long seed) {

    LOGGER.info("Starting simulation with {} users over {} days.", userCount,
        simulationTime.toDays());

    final Random r = new Random(seed);

    samplingWordPredecessorSuccessorDistribution.reseedRandomGenerator(seed);
    tweetNumberDistribution.reseedRandomGenerator(seed);
    tweetTimeDistribution.reseedRandomGenerator(seed);
    final int simulationDays = (int) simulationTime.toDays();

    final TWIGModelWrapper resultModel = new TWIGModelWrapper();

    // for each user
    for (int i = 0; i < userCount; i++) {
      final User user = new User();
      final Set<LocalDateTime> timeStamps = new HashSet<>();

      // number of tweet for the user
      final int tweetCount =
          (tweetNumberDistribution.sample() * simulationDays) / TWEET_NUMBER_NORMALIZATION_DAYS;
      LOGGER.info("User {} tweets {} times.", user.getNameAsHexString(), tweetCount);

      for (int d = 0; d < tweetCount; d++) {
        // find for each tweet a tweet time
        LocalDateTime tweetTime = null;
        while ((tweetTime == null) || timeStamps.contains(tweetTime)) {
          final LocalDate date = startDate.plusDays(r.nextInt(simulationDays));
          final LocalTime time = tweetTimeDistribution.sample().withSecond(r.nextInt(SECONDS));
          tweetTime = LocalDateTime.of(date, time);
        }
        timeStamps.add(tweetTime);

        // create tweet content
        final String tweetContent = samplingWordPredecessorSuccessorDistribution.sample();

        // add all to the model
        resultModel.addTweetNoAnonymization(user.getNameAsHexString(), tweetContent, tweetTime,
            Collections.emptyList(), seed);
      }

      // store created data
      if (resultModel.getModel().size() > Const.MODEL_MAX_SIZE) {
        LOGGER.info("Printing result model");
        write(resultModel);
      }
    }
    write(resultModel);
    return resultModel;
  }

  private void write(final TWIGModelWrapper resultModel) {
    try (FileWriter writer = new FileWriter(resultStoreFileHandler.nextFile())) {
      resultModel.write(writer);
    } catch (final IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * Executes {@link #simulate(int, Duration, LocalDate, long)} with following arguments:
   * <li>
   * <ul>
   * {@code arg[0]} must state a path to a serialized {@link WordMatrix}
   * </ul>
   * <ul>
   * {@code arg[1]} must state a path to a serialized {@link MessageCounter}
   * </ul>
   * <ul>
   * {@code arg[2]} must state a path to a serialized {@link TimeCounter}
   * </ul>
   * <ul>
   * {@code arg[3]} must state an integer value for {@code userCount}
   * </ul>
   * <ul>
   * {@code arg[4]} must state an integer value for {@code simulationTime}
   * </ul>
   * <ul>
   * {@code arg[5]} must state a date in format {@link DateTimeFormatter#ISO_LOCAL_DATE} as
   * {@code startDate}
   * </ul>
   * <ul>
   * {@code arg[6]} must state a long value for {@code seed}
   * </ul>
   * <ul>
   * {@code arg[7]} must state a directory in which the resulting file
   * {@code generated_twig_model_XXX.ttl} will be created with {@code _XXX} being a generic suffix
   * </ul>
   * </li>
   *
   * @param args Arguments as specified above.
   */
  public static void main(final String[] args) {

    if (args.length < 8) {
      throw new IllegalArgumentException("Insufficient arguments supplied");
    }

    // read parameters
    final String wordmatrixFile = args[0];
    final String messageCounterFile = args[1];
    final String timeCounterFile = args[2];

    final int userCount = Integer.parseInt(args[3]);
    final int days = Integer.parseInt(args[4]);
    final LocalDate startDate = LocalDate.from(DateTimeFormatter.ISO_LOCAL_DATE.parse(args[5]));
    final long seed = Long.parseLong(args[6]);
    final File f = new File(args[7]);
    if (!f.isDirectory()) {
      throw new IllegalArgumentException("Supplied file must be a directory");
    }

    // load models

    LOGGER.info("loads MessageCounter");
    SamplingDiscreteDistribution<Integer> messageDistribution;
    try (ObjectInputStream stream =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(messageCounterFile)))) {
      final MessageCounter messageCounter = (MessageCounter) stream.readObject();
      messageDistribution = messageCounter
          .normalize(Duration.ofDays(TWEET_NUMBER_NORMALIZATION_DAYS)).getValueDistribution();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      return;
    }

    LOGGER.info("loads TimeCounter");
    SamplingDiscreteDistribution<LocalTime> timeDistribution;
    try (ObjectInputStream stream =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(timeCounterFile)))) {
      final TimeCounter timeCounter = (TimeCounter) stream.readObject();
      timeDistribution = timeCounter.getValueDistribution();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      return;
    }

    LOGGER.info("loads WordMatrix");
    WordSampler wordSampler;
    try (ObjectInputStream stream =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(wordmatrixFile)))) {
      final WordMatrix wordMatrix = (WordMatrix) stream.readObject();
      wordSampler = new WordSampler(wordMatrix);
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.error(e.getMessage(), e);
      return;
    }

    LOGGER.info("loads automation");
    // starts automation
    final Automaton automaton;
    automaton = new Automaton(wordSampler, messageDistribution, timeDistribution, f);
    automaton.simulate(userCount, Duration.ofDays(days), startDate, seed);
  }
}
