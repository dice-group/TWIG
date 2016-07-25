package org.aksw.twig.model;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps a {@link Model} using TWIG ontology to create RDF-graphs.
 */
public class TwitterModelWrapper {

    // Prefix mappings
    private static final String FOAF_IRI = "http://xmlns.com/foaf/0.1/";
    private static final String FOAF_PREF = "foaf";
    private static final String TWIG_IRI = "http://aksw.org/twig#";
    private static final String TWIG_PREF = "twig";
    private static final String OWL_IRI = "http://www.w3.org/2002/07/owl#";
    private static final String OWL_PREF = "owl";
    private static final String RDF_IRI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final String RDF_PREF = "rdf";

    private static final PrefixMapping PREFIX_MAPPING = PrefixMapping.Factory.create();
    static
    {
        PREFIX_MAPPING.setNsPrefix(FOAF_PREF, FOAF_IRI);
        PREFIX_MAPPING.setNsPrefix(TWIG_PREF, TWIG_IRI);
        PREFIX_MAPPING.setNsPrefix(OWL_PREF, OWL_IRI);
        PREFIX_MAPPING.setNsPrefix(RDF_PREF, RDF_IRI);
    }

    // RDF statement parts.
    private static final Resource TWEET = ResourceFactory.createResource(PREFIX_MAPPING.expandPrefix("twig:Tweet"));
    private static final Resource ONLINE_TWITTER_ACCOUNT = ResourceFactory.createResource(PREFIX_MAPPING.expandPrefix("twig:OnlineTwitterAccount"));
    private static final Resource OWL_NAMED_INDIVIDUAL = ResourceFactory.createResource(PREFIX_MAPPING.expandPrefix("owl:NamedIndividual"));
    private static final Property SENDS = ResourceFactory.createProperty(PREFIX_MAPPING.expandPrefix("twig:sends"));
    private static final Property MENTIONS = ResourceFactory.createProperty(PREFIX_MAPPING.expandPrefix("twig:mentions"));
    private static final Property TWEET_TIME = ResourceFactory.createProperty(PREFIX_MAPPING.expandPrefix("twig:tweetTime"));
    private static final Property TWEET_CONTENT = ResourceFactory.createProperty(PREFIX_MAPPING.expandPrefix("twig:tweetContent"));
    private static final Property RDF_TYPE = ResourceFactory.createProperty(PREFIX_MAPPING.expandPrefix("rdf:type"));

    private static long userId = 0;

    private static Map<String, Long> userIdMapping = new ConcurrentHashMap<>();

    /** The wrapped model. */
    public final Model model = ModelFactory.createDefaultModel();

    /**
     * Creates a new instance along with a new {@link Model} to wrap.
     */
    public TwitterModelWrapper() {
        this.model.setNsPrefixes(PREFIX_MAPPING);
    }

    /**
     * Adds a tweet to the wrapped {@link Model}.
     * @param accountName Name of the tweeting account.
     * @param tweetContent Content of the tweet.
     * @param tweetTime Time of the tweet.
     * @param mentions All mentioned account names of the tweet.
     */
    public void addTweet(String accountName, String tweetContent, LocalDateTime tweetTime, Collection<String> mentions) {
        Resource twitterAccount = getTwitterAccount(accountName);

        String anonymizedTweetContent = tweetContent;
        for (String mention: mentions) {
            anonymizedTweetContent = anonymizedTweetContent.replaceAll(mention, anonymizeTwitterAccount(mention));
        }

        Resource tweet = this.model.getResource(createTweetIri(accountName, tweetTime))
                .addProperty(RDF_TYPE, OWL_NAMED_INDIVIDUAL)
                .addProperty(RDF_TYPE, TWEET)
                .addLiteral(TWEET_CONTENT, this.model.createTypedLiteral(anonymizedTweetContent))
                .addLiteral(TWEET_TIME, this.model.createTypedLiteral(tweetTime.toString(), XSDDatatype.XSDdateTime)); // TODO: add timezone

        twitterAccount.addProperty(SENDS, tweet);

        mentions.forEach(mention -> tweet.addProperty(MENTIONS, getTwitterAccount(mention)));
    }

    /**
     * Gets/creates a resource to a twitter account.
     * @param accountName Name of the twitter account.
     * @return Resource of the twitter account.
     */
    private Resour  ce getTwitterAccount(String accountName) {
        return this.model.getResource(createTwitterAccountIri(accountName))
                .addProperty(RDF_TYPE, OWL_NAMED_INDIVIDUAL)
                .addProperty(RDF_TYPE, ONLINE_TWITTER_ACCOUNT);
    }

    /**
     * Replaces a twitter account with a unique but non deterministic twitterUser_X.
     * @param twitterAccountName User account name.
     * @return Anonymized name.
     */
    private static String anonymizeTwitterAccount(String twitterAccountName) {
        long id = userIdMapping.computeIfAbsent(twitterAccountName, name -> userId++);
        return "twitterUser_".concat(Long.toString(id));
    }

    /**
     * Creates the IRI of a twitter account.
     * @param twitterAccountName Name of the account.
     * @return IRI of the twitter account.
     */
    private static String createTwitterAccountIri(String twitterAccountName) {
        return prefixedIri(anonymizeTwitterAccount(twitterAccountName));
    }

    /**
     * Creates the IRI of a tweet.
     * @param twitterAccountName Name of the tweeting account.
     * @param messageTime Date and time of the tweet.
     * @return IRI of the tweet.
     */
    private static String createTweetIri(String twitterAccountName, LocalDateTime messageTime) {
        StringBuilder builder = new StringBuilder(anonymizeTwitterAccount(twitterAccountName));
        builder.append('_');
        builder.append(messageTime.toString().replaceAll(":", "-"));
        return prefixedIri(builder.toString());
    }

    /**
     * Prefixes a string with the 'twig:' prefix.
     * @param original String to prefix.
     * @return Prefixed string.
     */
    private static String prefixedIri(String original) {
        StringBuilder builder = new StringBuilder(TWIG_IRI);
        builder.append(original);
        return builder.toString();
    }
}
