package org.aksw.twig.model;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps a {@link Model} using TWIG ontology to create RDF-graphs.
 */
public class TwitterModelWrapper {

    // Prefix mappings
    private static final String FOAF_IRI = "http://xmlns.com/foaf/0.1/";
    private static final String FOAF_PREF = "foaf";
    private static final String TWIG_IRI = "http://aksw.org/twig";
    private static final String TWIG_PREF = "twig";

    private static final Map<String, String> PREFIXES_MAPPING = new HashMap<>();
    static
    {
        PREFIXES_MAPPING.put(FOAF_PREF, FOAF_IRI);
        PREFIXES_MAPPING.put(TWIG_PREF, TWIG_IRI);
    }

    // RDF statement parts.
    private static final Resource TWEET = ResourceFactory.createResource("twig:Tweet");
    private static final Resource ONLINE_TWITTER_ACCOUNT = ResourceFactory.createResource("twig:OnlineTwitterAccount");
    private static final Resource OWL_NAMED_INDIVIDUAL = ResourceFactory.createResource("owl:NamedIndividual");
    private static final Property SENDS = ResourceFactory.createProperty("twig:sends");
    private static final Property MENTIONS = ResourceFactory.createProperty("twig:mentions");
    private static final Property TWEET_TIME = ResourceFactory.createProperty("twig:tweetTime");
    private static final Property TWEET_CONTENT = ResourceFactory.createProperty("twig:tweetContent");
    private static final Property RDF_TYPE = ResourceFactory.createProperty("rdf:type");

    /** The wrapped model. */
    public final Model model = ModelFactory.createDefaultModel();

    /**
     * Creates a new instance along with a new {@link Model} to wrap.
     */
    public TwitterModelWrapper() {
        this.model.setNsPrefixes(PREFIXES_MAPPING);
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

        Resource tweet = this.model.getResource(createTweetIri(accountName, tweetTime))
                .addProperty(RDF_TYPE, OWL_NAMED_INDIVIDUAL)
                .addProperty(RDF_TYPE, TWEET)
                .addLiteral(TWEET_CONTENT, this.model.createTypedLiteral(tweetContent))
                .addLiteral(TWEET_TIME, this.model.createTypedLiteral(tweetTime.toString(), XSDDatatype.XSDdateTime)); // TODO: add timezone

        twitterAccount.addProperty(SENDS, tweet);

        mentions.stream().forEach(mention -> tweet.addProperty(MENTIONS, getTwitterAccount(mention)));
    }

    /**
     * Gets/creates a resource to a twitter account.
     * @param accountName Name of the twitter account.
     * @return Resource of the twitter account.
     */
    private Resource getTwitterAccount(String accountName) {
        return this.model.getResource(createTwitterAccountIri(accountName))
                .addProperty(RDF_TYPE, OWL_NAMED_INDIVIDUAL)
                .addProperty(RDF_TYPE, ONLINE_TWITTER_ACCOUNT);
    }

    /**
     * Creates the IRI of a twitter account.
     * @param twitterAccountName Name of the account.
     * @return IRI of the twitter account.
     */
    private static String createTwitterAccountIri(String twitterAccountName) {
        return prefixedIri(twitterAccountName);
    }

    /**
     * Creates the IRI of a tweet.
     * @param twitterAccountName Name of the tweeting account.
     * @param messageTime Date and time of the tweet.
     * @return IRI of the tweet.
     */
    private static String createTweetIri(String twitterAccountName, LocalDateTime messageTime) {
        StringBuilder builder = new StringBuilder(twitterAccountName);
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
        StringBuilder builder = new StringBuilder(TWIG_PREF);
        builder.append(':');
        builder.append(original);
        return builder.toString();
    }
}
