package com.kutylo.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.io.IOException;

public class ConnectionConfig {

  private static final String HOST = "localhost";
  private static final int PORT_ONE = 9200;
  private static final String SCHEME = "http";

  private static RestHighLevelClient restHighLevelClient;

  public static synchronized RestHighLevelClient getConnection() {
    if (restHighLevelClient == null) {
      restHighLevelClient =
          new RestHighLevelClient(RestClient.builder(new HttpHost(HOST, PORT_ONE, SCHEME)));
    }

    return restHighLevelClient;
  }

  public static synchronized void closeConnection() throws IOException {
    restHighLevelClient.close();
    restHighLevelClient = null;
  }
}
