package org.aksw.twig.wordembeddings;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class Requests {
  private static final Logger LOGGER = LogManager.getLogger(Requests.class);
  public static final Charset UTF_8 = Charset.forName("UTF-8");

  public static String get(final String url) throws ClientProtocolException, IOException {
    return Request.Get(url)//
        .connectTimeout(1000).socketTimeout(1000)//
        .execute().returnContent().asString();
  }

  public static String postForm(final String url, final Form form, final ContentType ct)
      throws ClientProtocolException, IOException {

    final Response response = Request.Post(url) //
        .addHeader("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType()) //
        .addHeader("Accept-Charset", Requests.UTF_8.name()) //
        .addHeader("Accept", ct.getMimeType()) //
        .bodyForm(form.build()) //
        .execute();

    final HttpResponse httpResponse = response.returnResponse();
    LOGGER.info(httpResponse.getStatusLine());
    final HttpEntity entry = httpResponse.getEntity();
    final String r = IOUtils.toString(entry.getContent(), Requests.UTF_8);
    EntityUtils.consume(entry);
    return r;
  }

  public static String postJson(final String url, final JSONObject json, final ContentType ct)
      throws ClientProtocolException, IOException {
    final Response response = Request.Post(url) //
        .addHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType()) //
        .addHeader("Accept-Charset", Requests.UTF_8.name()) //
        .addHeader("Accept", ct.getMimeType()) //
        .bodyString(json.toString(), ContentType.APPLICATION_JSON) //
        .execute();
    final HttpResponse httpResponse = response.returnResponse();
    final HttpEntity entry = httpResponse.getEntity();
    final String r = IOUtils.toString(entry.getContent(), Requests.UTF_8);
    EntityUtils.consume(entry);
    return r;
  }
}
