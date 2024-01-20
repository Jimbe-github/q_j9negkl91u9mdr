package com.teratail.q_j9negkl91u9mdr;

import androidx.lifecycle.*;

import org.w3c.dom.*;

import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import javax.xml.parsers.*;
import javax.xml.xpath.*;

public class MainViewModel extends ViewModel {
  private static URL FEED_URL;
  static {
    try {
      FEED_URL = new URL("https://www.data.jma.go.jp/developer/xml/feed/extra.xml");
    } catch(MalformedURLException e) {
      e.printStackTrace();
    }
  }

  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  private MutableLiveData<Feed> feedLiveData = new MutableLiveData<>(null);
  LiveData<Feed> getFeed() { return feedLiveData; }

  private MutableLiveData<Exception> exceptionLiveData = new MutableLiveData<>(null);
  LiveData<Exception> getException() { return exceptionLiveData; }

  void requestFeed() {
    exceptionLiveData.setValue(null);
    executorService.execute(new XmlDownloadTask(FEED_URL, new FeedCallback()));
  }

  private class FeedCallback implements BiConsumer<Document,Exception> {
    @Override
    public void accept(Document document, Exception exception) {
      try {
        if(exception != null) throw exception;

        XPath xpath = XPathFactory.newInstance().newXPath();
        XPathExpression theTitle = xpath.compile("title/text()");
        XPathExpression theId = xpath.compile("id/text()");
        XPathExpression theUpdated = xpath.compile("updated/text()");
        XPathExpression theAuthorName = xpath.compile("author/name/text()");
        XPathExpression theLink = xpath.compile("link[@type='application/xml']/@href");
        XPathExpression theContentText = xpath.compile("content[@type='text']/text()");

        List<Feed.Entry> list = new ArrayList<>();

        NodeList nodeList = (NodeList) xpath.compile("/feed/entry").evaluate(document, XPathConstants.NODESET);
        for(int i=0; i<nodeList.getLength(); i++) {
          Element elm = (Element) nodeList.item(i);

          String title = theTitle.evaluate(elm);
          String id = theId.evaluate(elm);
          LocalDateTime updated = null;
          try {
            ZonedDateTime updatedZ = ZonedDateTime.parse(theUpdated.evaluate(elm), DateTimeFormatter.ISO_DATE_TIME);
            updated = updatedZ.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
          } catch(Exception e) {
            e.printStackTrace();
          }
          String authorName = theAuthorName.evaluate(elm);
          URL link = null;
          try {
            link = new URL(theLink.evaluate(elm));
          } catch(MalformedURLException e) {
            e.printStackTrace();
          }
          String contentText = theContentText.evaluate(elm);
          list.add(new Feed.Entry(title, id, updated, authorName, link, contentText));
        }

        String title = xpath.compile("/feed/title/text()").evaluate(document);
        LocalDateTime updated = null;
        try {
          OffsetDateTime updatedO = OffsetDateTime.parse(xpath.compile("/feed/updated/text()").evaluate(document), DateTimeFormatter.ISO_DATE_TIME);
          updated = updatedO.toLocalDateTime();
        } catch(Exception e) {
          e.printStackTrace();
        }

        Feed feed = new Feed(title, updated, list);
        feedLiveData.postValue(feed);
      } catch(Exception e) {
        exceptionLiveData.postValue(e);
      }
    }
  }

  @Override
  protected void onCleared() {
    executorService.shutdownNow();
    super.onCleared();
  }
}

class Feed {
  static class Entry {
    final String title;
    final String id;
    final LocalDateTime updated;
    final String authorName;
    final URL link;
    final String contentText;

    Entry(String title, String id, LocalDateTime updated, String authorName, URL link, String contentText) {
      this.title = title;
      this.id = id;
      this.updated = updated;
      this.authorName = authorName;
      this.link = link;
      this.contentText = contentText;
    }
  }

  final String title;
  final LocalDateTime updated;
  final List<Entry> entryList;

  Feed(String title, LocalDateTime updated, List<Entry> entryList) {
    this.title = title;
    this.updated = updated;
    this.entryList = Collections.unmodifiableList(new ArrayList<>(entryList)); //防御コピー
  }
}

class XmlDownloadTask implements Runnable {
  private static final String LOG_TAG = "XmlDownloadTask";

  private final URL url;
  private final BiConsumer<Document,Exception> callback;

  XmlDownloadTask(URL url, BiConsumer<Document,Exception> callback) {
    this.url = url;
    this.callback = callback;
  }

  @Override
  public void run() {
    try {
      HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
      try(InputStream is = new BufferedInputStream(urlConnection.getInputStream())) {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        callback.accept(docBuilder.parse(is), null);
      } finally {
        urlConnection.disconnect();
      }
    } catch(Exception e) {
      e.printStackTrace();
      callback.accept(null, e);
    }
  }
}