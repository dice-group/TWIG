package org.aksw.twig.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.aksw.twig.files.FileHandler;
import org.apache.commons.codec.binary.Hex;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wraps a {@link Model} using TWIG ontology to create RDF-graphs.
 */
public class TWIGModelWrapper {

  private static final Logger LOGGER = LogManager.getLogger(TWIGModelWrapper.class);

  public static final String LANG = "Turtle";

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  // Prefix mappings
  private static final String FOAF_IRI = "http://xmlns.com/foaf/0.1/";
  private static final String FOAF_PREF = "foaf";

  private static final String TWIG_IRI = "http://aksw.org/twig#";
  private static final String TWIG_PREF = "twig";

  private static final String OWL_IRI = "http://www.w3.org/2002/07/owl#";
  private static final String OWL_PREF = "owl";

  private static final String RDF_IRI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  private static final String RDF_PREF = "rdf";

  private static final String XSD_IRI = "http://www.w3.org/2001/XMLSchema#";
  private static final String XSD_REF = "xsd";

  private static final PrefixMapping PREFIX_MAPPING = PrefixMapping.Factory.create();

  static {
    PREFIX_MAPPING.setNsPrefix(FOAF_PREF, FOAF_IRI);
    PREFIX_MAPPING.setNsPrefix(TWIG_PREF, TWIG_IRI);
    PREFIX_MAPPING.setNsPrefix(OWL_PREF, OWL_IRI);
    PREFIX_MAPPING.setNsPrefix(RDF_PREF, RDF_IRI);
    PREFIX_MAPPING.setNsPrefix(XSD_REF, XSD_IRI);
  }

  // RDF local names
  public static final String SENDS_PROPERTY_NAME = "sends";
  public static final String MENTIONS_PROPERTY_NAME = "mentions";
  public static final String TWEET_TIME_PROPERTY_NAME = "tweetTime";
  public static final String TWEET_CONTENT_PROPERTY_NAME = "tweetContent";

  // RDF statement parts.
  private static final Resource TWEET =
      ResourceFactory.createResource(PREFIX_MAPPING.expandPrefix("twig:Tweet"));
  private static final Resource ONLINE_TWITTER_ACCOUNT =
      ResourceFactory.createResource(PREFIX_MAPPING.expandPrefix("twig:OnlineTwitterAccount"));
  private static final Resource OWL_NAMED_INDIVIDUAL =
      ResourceFactory.createResource(PREFIX_MAPPING.expandPrefix("owl:NamedIndividual"));
  private static final Property SENDS = ResourceFactory
      .createProperty(PREFIX_MAPPING.expandPrefix("twig:".concat(SENDS_PROPERTY_NAME)));
  private static final Property MENTIONS = ResourceFactory
      .createProperty(PREFIX_MAPPING.expandPrefix("twig:".concat(MENTIONS_PROPERTY_NAME)));
  private static final Property TWEET_TIME = ResourceFactory
      .createProperty(PREFIX_MAPPING.expandPrefix("twig:".concat(TWEET_TIME_PROPERTY_NAME)));
  private static final Property TWEET_CONTENT = ResourceFactory
      .createProperty(PREFIX_MAPPING.expandPrefix("twig:".concat(TWEET_CONTENT_PROPERTY_NAME)));
  private static final Property RDF_TYPE =
      ResourceFactory.createProperty(PREFIX_MAPPING.expandPrefix("rdf:type"));

  private static byte[] randomHashSuffix = new byte[32];

  static {
    new Random().nextBytes(randomHashSuffix);
  }

  private MessageDigest MD5;

  /** The wrapped model. */
  private Model model = ModelFactory.createDefaultModel();

  public Model getModel() {
    return model;
  }

  /**
   * Creates a new instance along with a new {@link Model} to wrap.
   */
  public TWIGModelWrapper() {
    model.setNsPrefixes(PREFIX_MAPPING);
    try {
      MD5 = MessageDigest.getInstance("MD5");
    } catch (final NoSuchAlgorithmException e) {
      throw new ExceptionInInitializerError();
    }
  }

  /**
   * Adds a tweet to the wrapped {@link Model}.
   * 
   * @param accountName Name of the tweeting account.
   * @param tweetContent Content of the tweet.
   * @param tweetTime Time of the tweet.
   * @param mentions All mentioned account names of the tweet.
   */
  public void addTweet(final String accountName, final String tweetContent,
      final LocalDateTime tweetTime, final Collection<String> mentions) {
    final Set<String> anonymizedMentions = new HashSet<>();
    String anonymizedTweetContent = tweetContent;
    for (final String mention : mentions) {
      final String anonymizedMention = anonymizeTwitterAccount(mention);
      anonymizedMentions.add(anonymizedMention);
      anonymizedTweetContent = anonymizedTweetContent.replaceAll(mention, anonymizedMention);
    }

    addTweetNoAnonymization(anonymizeTwitterAccount(accountName), anonymizedTweetContent, tweetTime,
        anonymizedMentions);
  }

  /**
   * Same as {@link #addTweet(String, String, LocalDateTime, Collection)} but with no username
   * anonymization. Useful for already anonymized data.
   * 
   * @param accountName See original documentation.
   * @param tweetContent See original documentation.
   * @param tweetTime See original documentation.
   * @param mentions See original documentation.
   */
  public void addTweetNoAnonymization(final String accountName, final String tweetContent,
      final LocalDateTime tweetTime, final Collection<String> mentions) {
    final Resource twitterAccount = getTwitterAccount(accountName);

    final Resource tweet = model.getResource(createTweetIri(accountName, tweetTime))
        .addProperty(RDF_TYPE, OWL_NAMED_INDIVIDUAL).addProperty(RDF_TYPE, TWEET)
        .addLiteral(TWEET_CONTENT, model.createTypedLiteral(tweetContent))
        .addLiteral(TWEET_TIME, model.createTypedLiteral(tweetTime.format(DATE_TIME_FORMATTER),
            XSDDatatype.XSDdateTime)); // TODO: add timezone

    twitterAccount.addProperty(SENDS, tweet);

    mentions.forEach(mention -> tweet.addProperty(MENTIONS, getTwitterAccount(mention)));
  }

  /**
   * Gets/creates a resource to a twitter account.
   * 
   * @param accountName Name of the twitter account.
   * @return Resource of the twitter account.
   */
  private Resource getTwitterAccount(final String accountName) {
    return model.getResource(createTwitterAccountIri(accountName))
        .addProperty(RDF_TYPE, OWL_NAMED_INDIVIDUAL).addProperty(RDF_TYPE, ONLINE_TWITTER_ACCOUNT);
  }

  /**
   * Replaces a twitter account with a unique but non deterministic twitterUser_X.
   * 
   * @param twitterAccountName User account name.
   * @return Anonymized name.
   */
  private String anonymizeTwitterAccount(final String twitterAccountName) {
    MD5.update(twitterAccountName.getBytes());
    MD5.update(randomHashSuffix);
    byte[] hash;
    try {
      hash = MD5.digest();
    } catch (final RuntimeException e) {
      LOGGER.error("Exception during anonymizing {}", twitterAccountName);
      return null;
    }
    return Hex.encodeHexString(hash);
  }

  /**
   * Creates the IRI of a twitter account.
   * 
   * @param twitterAccountName Name of the account.
   * @return IRI of the twitter account.
   */
  private String createTwitterAccountIri(final String twitterAccountName) {
    return prefixedIri(twitterAccountName);
  }

  /**
   * Creates the IRI of a tweet.
   * 
   * @param twitterAccountName Name of the tweeting account.
   * @param messageTime Date and time of the tweet.
   * @return IRI of the tweet.
   */
  private String createTweetIri(final String twitterAccountName, final LocalDateTime messageTime) {
    final String returnValue =
        twitterAccountName.concat("_").concat(messageTime.toString().replaceAll(":", "-"));
    return prefixedIri(returnValue);
  }

  /**
   * Prefixes a string with the 'twig:' prefix.
   * 
   * @param original String to prefix.
   * @return Prefixed string.
   */
  private static String prefixedIri(final String original) {
    return TWIG_IRI.concat(original);
  }

  /**
   * Writes the model into the given writer and deletes the current one. <b>No</b> other methods
   * (such as {@link Writer#flush()}) are invoked at the writer.
   * 
   * @param writer Writer to write in.
   */
  public void write(final Writer writer) {
    model.write(writer, LANG);
    model = ModelFactory.createDefaultModel();
  }

  /**
   * Reads a TWIG rdf model from a file.
   * 
   * @param file File to read from.
   * @return TWIGModelWrapper
   * @throws IOException IO error.
   */
  public static TWIGModelWrapper read(final File file) throws IOException {
    final TWIGModelWrapper wrapper = new TWIGModelWrapper();
    try (InputStream inputStream = FileHandler.getDecompressionStreams(file)) {
      wrapper.model.read(inputStream, null, LANG);
      return wrapper;
    }
  }
}
