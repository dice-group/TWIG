package org.aksw.twig.parsing;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.rdf.model.Model;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Parses a {@link org.apache.jena.rdf.model.Model} from a twitter7 block triple.
 * @author Felix Linker
 */
public class Twitter7BlockParser implements Callable<Model> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String TWITTER_AUTHORITY = "twitter.com";

    /** T line of the twitter7 block. */
    private String lineT;

    /** Parsed timestamp form T line. */
    private LocalDateTime messageDateTime;

    /**
     * Getter to parsed timestamp.
     * @return Timestamp.
     */
    public LocalDateTime getMessageDateTime() {
        return messageDateTime;
    }

    /** U line of the twitter7 block. */
    private String lineU;

    /** Parsed twitter user name from U line. */
    private String twitterUserName;

    /**
     * Getter to parsed twitter username.
     * @return Twitter username.
     */
    public String getTwitterUserName() {
        return twitterUserName;
    }

    /** W line from twittter7 block. */
    private String lineW;

    /** Parsed message content from W line. */
    private String messageContent;

    /**
     * Getter to parsed message content.
     * @return Message content.
     */
    public String getMessageContent() {
        return messageContent;
    }

    /**
     * Creates a new parser for given triple. Triple must contain twitter7 data for one block.
     * @param twitter7Triple Triple to parse.
     */
    public Twitter7BlockParser(Triple<String, String, String> twitter7Triple) {
        this.lineT = twitter7Triple.getLeft();
        this.lineU = twitter7Triple.getMiddle();
        this.lineW = twitter7Triple.getRight();
    }

    @Override
    public Model call() throws Twitter7BlockParseException {

        // Parse date and time
        try {
            this.messageDateTime = LocalDateTime.from(DATE_TIME_FORMATTER.parse(this.lineT.trim()));
        } catch (DateTimeException e) {
            throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.DATETIME_MALFORMED);
        }

        // Parse user name
        try {
            URL twitterUrl = new URL(this.lineU);
            if (!twitterUrl.getAuthority().equalsIgnoreCase(TWITTER_AUTHORITY)) {
                throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.NO_TWITTER_LINK);
            }

            this.twitterUserName = Arrays.stream(twitterUrl.getPath().split("/"))
                    .filter(pathPart -> !pathPart.isEmpty())
                    .findFirst()
                    .orElse(null);

            if (this.twitterUserName == null) {
                throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.NO_TWITTER_ACCOUNT);
            }
        } catch (MalformedURLException e) {
            throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.URL_MALFORMED);
        }

        // Parse message content
        this.messageContent = this.lineW.trim();

        Model model = Twitter7ModelFactory.createModel();

        return model;
    }
}
