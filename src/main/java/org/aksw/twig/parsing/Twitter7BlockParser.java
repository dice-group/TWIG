package org.aksw.twig.parsing;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aksw.twig.Const;
import org.aksw.twig.model.TWIGModelWrapper;
import org.apache.commons.lang3.tuple.Triple;

/**
 * Parses a {@link TWIGModelWrapper} from a twitter7 block triple.
 *
 * @author Felix Linker
 */
class Twitter7BlockParser implements Callable<TWIGModelWrapper> {

  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String TWITTER_AUTHORITY = "twitter.com";

  private static final Pattern MENTIONS_PATTERN = Pattern.compile("@([a-zA-Z0-9_]{1,15})");

  /** T line of the twitter7 block. */
  private final String lineT;

  /** Parsed timestamp form T line. */
  private LocalDateTime messageDateTime;

  /**
   * Getter to parsed timestamp.
   *
   * @return Timestamp.
   */
  LocalDateTime getMessageDateTime() {
    return messageDateTime;
  }

  /** U line of the twitter7 block. */
  private final String lineU;

  /** Parsed twitter user name from U line. */
  private String twitterUserName;

  /**
   * Getter to parsed twitter username.
   *
   * @return Twitter username.
   */
  String getTwitterUserName() {
    return twitterUserName;
  }

  /** W line from twitter7 block. */
  private final String lineW;

  /** Parsed message content from W line. */
  private String messageContent;

  /** Parsed '@' twitter username mentions from message content. */
  private final Collection<String> mentions = new LinkedList<>();

  /**
   * Getter to parsed message content.
   *
   * @return Message content.
   */
  String getMessageContent() {
    return messageContent;
  }

  /**
   * Creates a new parser for given triple. Triple must contain twitter7 data for one block.
   *
   * @param twitter7Triple Triple to parse.
   */
  Twitter7BlockParser(final Triple<String, String, String> twitter7Triple) {
    lineT = twitter7Triple.getLeft();
    lineU = twitter7Triple.getMiddle();
    lineW = twitter7Triple.getRight();
  }

  @Override
  public TWIGModelWrapper call() throws Twitter7BlockParseException {

    // Parse date and time
    try {
      messageDateTime = LocalDateTime.from(DATE_TIME_FORMATTER.parse(lineT.trim()));
    } catch (final DateTimeException e) {
      throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.DATETIME_MALFORMED);
    }

    // Parse user name
    try {
      final URL twitterUrl = new URL(lineU);
      if (!twitterUrl.getAuthority().equalsIgnoreCase(TWITTER_AUTHORITY)) {
        throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.NO_TWITTER_LINK);
      }

      twitterUserName = Arrays.stream(twitterUrl.getPath().split("/"))
          .filter(pathPart -> !pathPart.isEmpty()).findFirst().orElse(null);

      if (twitterUserName == null) {
        throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.NO_TWITTER_ACCOUNT);
      }
    } catch (final MalformedURLException e) {
      throw new Twitter7BlockParseException(Twitter7BlockParseException.Error.URL_MALFORMED);
    }
    // Parse message content
    messageContent = lineW.trim();

    final Matcher mentionsMatcher = MENTIONS_PATTERN.matcher(messageContent);
    while (mentionsMatcher.find()) {
      mentions.add(mentionsMatcher.group(1));
    }

    final TWIGModelWrapper model = new TWIGModelWrapper();
    model.addTweet(twitterUserName, messageContent, messageDateTime, mentions, Const.SEED);
    return model;
  }
}
