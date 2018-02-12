package org.aksw.twig.parsing;

/**
 * Exception thrown by {@link Twitter7BlockParser} upon malformed data.
 * 
 * @author Felix Linker
 */
class Twitter7BlockParseException extends Exception {

  enum Error {
    DATETIME_MALFORMED, NO_TWITTER_ACCOUNT, NO_TWITTER_LINK, URL_MALFORMED
  }

  private static final String ERROR_MSG_DATETIME_MALFORMED =
      "The twitter7 block contained a malformed datetime line.";
  private static final String ERROR_MSG_NO_TWITTER_ACCOUNT =
      "The twitter7 block didn't contain a twitter account.";
  private static final String ERROR_MSG_NO_TWITTER_LINK =
      "The twitter7 block didn't contain a twitter link.";
  private static final String ERROR_MSG_URL_MALFORMED =
      "The twitter7 block contained a malformed URL to the twitter account.";

  /**
   * Creates a new exception with error type.
   * 
   * @param error Error type.
   */
  Twitter7BlockParseException(Error error) {
    super(getErrorMessage(error));
  }

  private static String getErrorMessage(Error error) {
    switch (error) {
      case DATETIME_MALFORMED:
        return ERROR_MSG_DATETIME_MALFORMED;
      case NO_TWITTER_ACCOUNT:
        return ERROR_MSG_NO_TWITTER_ACCOUNT;
      case NO_TWITTER_LINK:
        return ERROR_MSG_NO_TWITTER_LINK;
      case URL_MALFORMED:
        return ERROR_MSG_URL_MALFORMED;
      default:
        throw new IllegalArgumentException();
    }
  }
}
