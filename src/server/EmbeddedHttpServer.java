package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
import java.util.*;

import com.sun.net.httpserver.*;

import fig.html.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

import smartAutocomplete.*;
import smartAutocomplete.util.*;

public class EmbeddedHttpServer {
  private static HtmlUtils H = new HtmlUtils();

  private HttpServer server;

  private KillHandler killHandler = new KillHandler();

  public EmbeddedHttpServer(Exec exec) {
    try {
      server = HttpServer.create(new InetSocketAddress(8000), 0);
      server.createContext("/", new StaticHandler());
      server.createContext("/index.html", new ListHandler(exec));
      CodeHandler codeHandler = new CodeHandler(exec);
      server.createContext("/code", codeHandler);
      server.createContext("/json", codeHandler);
      server.createContext("/kill", killHandler);
      server.setExecutor(null); // creates a default executor
    } catch (IOException e) {
      logs("Couldn't start server because " + e.toString());
      System.exit(-1);
    }
  }

  public void start() {
    begin_track("Web-based error analysis");
    logs("Server started...");
    server.start();
    try {
      killHandler.waitForKill();
    } catch (InterruptedException e) {
      logs("Couldn't start server because " + e.toString());
      System.exit(-1);
    }
    logs("Server killed.");
    end_track();
  }

  static class ListHandler implements HttpHandler {
    private Exec exec;

    public ListHandler(Exec exec) {
      this.exec = exec;
    }

    public void handle(HttpExchange t) throws IOException {
      Http.send(t, "text/html",
                exec.getListHtml(t.getRequestURI().getQuery()));
    }
  }

  static class CodeHandler implements HttpHandler {
    private Exec exec;

    public CodeHandler(Exec exec) {
      this.exec = exec;
    }

    public void handle(HttpExchange t) throws IOException {
      URI uri = t.getRequestURI();
      boolean isCode = uri.getPath().substring(0,6).equals("/code/");
      String path = uri.getPath().substring(6);
      if (isCode) {
        logs("Received request for code of %s", path);
      } else {
        logs("Received request for info about %s", path);
      }
      String response = isCode ? CodeHtml.getHtml(path) : exec.getJson(path);
      String contentType = isCode ? "text/html" : "application/json";
      Http.send(t, contentType, response);
    }
  }
}
