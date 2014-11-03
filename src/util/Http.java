package smartAutocomplete.util;

import java.io.*;
import java.util.*;
import java.net.*;

import com.sun.net.httpserver.*;

public class Http {
  static public void send(HttpExchange t, String contentType,
                          String response) throws IOException {
    Headers responseHeaders = t.getResponseHeaders();
    responseHeaders.set("Content-Type", contentType);
    t.sendResponseHeaders(200, response.length());

    OutputStream os = t.getResponseBody();
    os.write(response.getBytes());

    t.close();
  }

  static public void sendError(HttpExchange t) throws IOException {
    String response = "404 (Not Found)\n";
    t.sendResponseHeaders(404, response.length());
    OutputStream os = t.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    String query = url.getQuery();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    return query_pairs;
  }
}
