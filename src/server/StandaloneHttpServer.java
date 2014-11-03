package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.sun.net.httpserver.*;

import fig.html.*;
import static fig.basic.LogInfo.logs;

import smartAutocomplete.*;
import smartAutocomplete.util.*;

public class StandaloneHttpServer {
  private static HtmlUtils H = new HtmlUtils();

  static class ListHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      URI uri = t.getRequestURI();
      Exec exec = Exec.getExec(uri.getPath().substring(6));
      if (exec == null) {
        Http.sendError(t);
        return;
      }

      Http.send(t, "text/html", exec.getListHtml(uri.getQuery()));
    }
  }

  static class InfoHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      URI uri = t.getRequestURI();
      String prefix = uri.getPath().substring(6);
      int slashIdx = prefix.indexOf("/");
      String path = prefix.substring(slashIdx+1);
      String execName = prefix.substring(0, slashIdx);
      String queryStr = uri.getQuery();

      logs("Received request for info about %s", path);

      Exec exec = Exec.getExec(execName);
      if (exec == null) {
        Http.sendError(t);
        return;
      }

      StringBuilder response = new StringBuilder();
      response.append("[");
      response.append(exec.getJson(path));
      logs("query: %s", queryStr);
      if (queryStr != null) {
        String[] altExecs = queryStr.substring(4).split(",");
        for (String execStr : altExecs) {
          exec = Exec.getExec(execStr);
          if (exec == null) {
            Http.sendError(t);
            return;
          }
          response.append(",");
          response.append(exec.getJson(path));
        }
        logs("altExecs: %s", (Object)altExecs);
      }
      response.append("]");

      Http.send(t, "application/json", response.toString());
    }
  }

  static class CodeHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException {
      String prefix = t.getRequestURI().getPath().substring(6);
      String path = prefix.substring(prefix.indexOf("/")+1);

      logs("Received request for code of %s", path);
      Http.send(t, "text/html", CodeHtml.getHtml(path));
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) throw new RuntimeException("Must supply statePath");
    String statePath = args[0];
    Exec.statePath = statePath.endsWith("/") ?
                     statePath.substring(0, statePath.length()-1) : statePath;

    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(8040), 32);
      server.createContext("/", new StaticHandler());
      server.createContext("/exec", new ListHandler());
      server.createContext("/code", new CodeHandler());
      server.createContext("/json", new InfoHandler());
      server.setExecutor(Executors.newFixedThreadPool(64));
      server.start();
      logs("Server started...");
    } catch (IOException e) {
      logs("Couldn't start server because " + e.toString());
      System.exit(-1);
    }
  }
}
