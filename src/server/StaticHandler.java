package smartAutocomplete.httpServer;

import java.io.*;
import java.net.*;
// import java.util.*;

import static fig.basic.LogInfo.logs;

import com.sun.net.httpserver.*;

import smartAutocomplete.util.*;

class StaticHandler implements HttpHandler {
  private final String rootPath = "./webapp";
  private File rootFile;

  public StaticHandler() {
    try {
      rootFile = new File(rootPath).getCanonicalFile();
    } catch (IOException e) {
      logs("Couldn't start server because " + e.toString());
      System.exit(-1);
    }
  }

  public void handle(HttpExchange t) throws IOException {
    URI uri = t.getRequestURI();
    String path = rootPath + uri.getPath();
    logs("Looking for: %s", path);
    File file = new File(path).getCanonicalFile();

    if (!file.isFile() || !FileUtil.contains(rootFile, file)) {
      // Object does not exist or is not a file: reject with 404 error.
      String response = "404 (Not Found)\n";
      t.sendResponseHeaders(404, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    } else {
      // Object exists and is a file: accept with response code 200.
      String mime = "text/html";
      if(path.endsWith(".js")) mime = "application/javascript";
      if(path.endsWith(".css")) mime = "text/css";            
      if(path.endsWith(".ttf")) mime = "application/x-font-ttf";            

      Headers h = t.getResponseHeaders();
      h.set("Content-Type", mime);
      t.sendResponseHeaders(200, 0);              

      OutputStream os = t.getResponseBody();
      FileInputStream fs = new FileInputStream(file);
      final byte[] buffer = new byte[0x10000];
      int count = 0;
      while ((count = fs.read(buffer)) >= 0) {
        os.write(buffer,0,count);
      }
      fs.close();
      os.close();
    }  
  }
}

