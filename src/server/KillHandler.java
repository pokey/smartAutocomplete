package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;

import com.sun.net.httpserver.*;

class KillHandler implements HttpHandler {
  boolean ready = false;

  void waitForKill() throws InterruptedException {
    synchronized(this) {
      while (!ready) wait();
    }
  }

  public void handle(HttpExchange t) throws IOException {
    URI uri = t.getRequestURI();
    String response = "Server killed.";

    Headers responseHeaders = t.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    t.sendResponseHeaders(200, response.length());

    OutputStream os = t.getResponseBody();
    os.write(response.getBytes());

    t.close();
    synchronized(this) {
      ready = true;
      notifyAll();
    }
  }
}
