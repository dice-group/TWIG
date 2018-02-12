package org.aksw.twig.parsing;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

public class Twitter7ParserTest {

  private static final String SAMPLE = "T       2009-09-30 23:55:53\n"
      + "U       http://twitter.com/user1\n"
      + "W       I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)\n"
      + "\n" + "T       2009-09-30 23:55:53\n" + "U       http://twitter.com/user2\n"
      + "W       soooo i got sum advice from the 1 i love most...he goes drop all those lame ass birds uno unot like them...lol here it goes\n";

  private static final String SAMPLE_BROKEN =
      "T       2009-09-30 23:55:53\n" + "X       http://twitter.com/user1\n"
          + "W       I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)\n"
          + "\n" + "T       2009-09-30 23:55:53\n"
          + "W       soooo i got sum advice from the 1 i love most...he goes drop all those lame ass birds uno unot like them...lol here it goes\n"
          + "U       http://twitter.com/user2\n" + "\n" + "T       2009-09-30 23:55:53\n"
          + "W       @user4 PAAAUULLLL! I miss youuu! Lol I thought I sent you an email, but it was placed in my Drafts so I'll resend that to you! =]\n"
          + "\n" + "T       2009-09-30 23:55:53\n" + "U       http://twitter.com/user7\n"
          + "W       I'm writing my first twitter!!\n";

  private static final String SAMPLE_EMPTY = "";

  private static final Set<Triple<String, String, String>> EXPECTED_RESULTS = new HashSet<>();

  @Test
  public void readTest() {
    EXPECTED_RESULTS.clear();
    EXPECTED_RESULTS.add(new ImmutableTriple<>("       2009-09-30 23:55:53",
        "       http://twitter.com/user1",
        "I'm starting to feel really sick, hope is not the S**** flu! (That's the new S-word)"));
    EXPECTED_RESULTS.add(new ImmutableTriple<>("       2009-09-30 23:55:53",
        "       http://twitter.com/user2",
        "       soooo i got sum advice from the 1 i love most...he goes drop all those lame ass birds uno unot like them...lol here it goes"));

    try {
      InputStream inputStream = new ByteArrayInputStream(SAMPLE.getBytes());
      Twitter7Parser<Triple<String, String, String>> parser =
          new Twitter7Parser<>(inputStream, ParserCallable::new);
      parser.addFutureCallbacks(new Callback());
      parser.addParsingFinishedResultListeners(() -> Assert.assertTrue(EXPECTED_RESULTS.isEmpty()),
          () -> {
            try {
              inputStream.close();
            } catch (IOException e) {
              Assert.fail(e.getMessage());
            }
          });
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void readEmptyTest() {
    try {
      InputStream inputStream = new ByteArrayInputStream(SAMPLE_EMPTY.getBytes());
      Twitter7Parser<Triple<String, String, String>> parser =
          new Twitter7Parser<>(inputStream, ParserCallable::new);
      parser.addFutureCallbacks(new EmptyExpectingCallback());
      parser.addParsingFinishedResultListeners(() -> {
        try {
          inputStream.close();
        } catch (IOException e) {
          Assert.fail(e.getMessage());
        }
      });
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void readBrokenTest() {
    EXPECTED_RESULTS.clear();
    EXPECTED_RESULTS.add(new ImmutableTriple<>("       2009-09-30 23:55:53",
        "       http://twitter.com/user7", "       I'm writing my first twitter!!"));

    try {
      InputStream inputStream = new ByteArrayInputStream(SAMPLE_BROKEN.getBytes());
      Twitter7Parser<Triple<String, String, String>> parser =
          new Twitter7Parser<>(inputStream, ParserCallable::new);
      parser.addFutureCallbacks(new Callback());
      parser.addParsingFinishedResultListeners(() -> Assert.assertTrue(EXPECTED_RESULTS.isEmpty()),
          () -> {
            try {
              inputStream.close();
            } catch (IOException e) {
              Assert.fail(e.getMessage());
            }
          });
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  private class ParserCallable implements Callable<Triple<String, String, String>> {

    private Triple<String, String, String> arg;

    ParserCallable(Triple<String, String, String> arg) {
      this.arg = arg;
    }

    @Override
    public Triple<String, String, String> call() throws Exception {
      return arg;
    }
  }

  private class Callback implements FutureCallback<Triple<String, String, String>> {
    @Override
    public void onSuccess(Triple<String, String, String> result) {
      boolean removed = EXPECTED_RESULTS.remove(result);
      Assert.assertTrue("Expected results didn't contain succeeding result.", removed);
    }

    @Override
    public void onFailure(Throwable t) {
      Assert.fail();
    }
  }

  private class EmptyExpectingCallback implements FutureCallback<Triple<String, String, String>> {
    @Override
    public void onSuccess(Triple<String, String, String> result) {
      Assert.fail("Callback got invoked.");
    }

    @Override
    public void onFailure(Throwable t) {
      Assert.fail("Callback got invoked.");
    }
  }
}
