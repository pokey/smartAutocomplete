package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

import com.sun.net.httpserver.*;

import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

class ForwardHandler implements HttpHandler {
  private final String prefix;
  private final ExecutorService executor = Executors.newFixedThreadPool(1);

  public ForwardHandler(String prefix) {
    this.prefix = prefix;
  }

  private static class Request implements Runnable {
    private URL url;
    private String content;

    public Request(URL url, String content) {
      this.url = url;
      this.content = content;
    }

    @Override
    public void run() {
      try {
        URLConnection conn = url.openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);

        OutputStreamWriter os =
          new OutputStreamWriter(conn.getOutputStream());
        os.write(content);
        os.close();

        BufferedReader in =
          new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null);
        in.close();
      } catch (IOException e) {
        System.err.println("IOException " + e);
        System.exit(1);
      }
    }
  }

  public void forward(URI original, String content) {
    if (prefix == null || prefix.equals("")) return;

    try {
      URL url = new URL(prefix + original.getRawPath() +
                        "?" + original.getRawQuery());
      executor.execute(new Request(url, content));
    } catch (MalformedURLException e) {
      System.err.println("Exception " + e);
      System.exit(1);
    }
  }

  public void handle(HttpExchange t) throws IOException {
    forward(t.getRequestURI(),
            IOUtil.convertStreamToString(t.getRequestBody()));
    Http.send(t, "text/plain", "Thanks");
  }
}

