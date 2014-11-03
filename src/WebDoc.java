package smartAutocomplete;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;

import net.htmlescape.HtmlEscape;

import static fig.basic.LogInfo.logs;

public class WebDoc {
  private StringBuilder html = new StringBuilder();
  private StringBuilder json = new StringBuilder(); 
  private String jsonSep = "";
  private final String path;
  private final String text;
  private int lastLoc;

  public String getPath() { return path; }

  public WebDoc(String path) {
    this.path = path;
    this.lastLoc = 0;
    try {
      // FixMe: [performance] Don't repeat reading file
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      text = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    html.append("<html>");
    html.append("<head>");
    html.append("<title>" + path + "</title>");
    html.append("<link rel='stylesheet' type='text/css' href='/smartAutocomplete.css' />");
    html.append("<script src='/jquery.js'></script>");
    html.append("<script src='/smartAutocomplete.js'></script>");
    html.append("</head>");
    html.append("<body>");
    html.append("<pre style='margin: 0; line-height: 125%'>");

    json.append("[");
  }

  public void addToken(InferState state) {
    Token token = state.getTrueToken();
    html.append(text.substring(lastLoc, token.loc()));
    lastLoc = token.loc() + token.str().length();
    html.append("<span id='t" + token.loc() + "'>" + HtmlEscape.escape(token.str()) + 
                "</span>");
    String entropy;
    if (!state.isIdent()) {
      entropy = "-2";
    } else if (!state.isOracle()) {
      entropy = "-1";
    } else {
      entropy = String.valueOf(state.getEntropy());
    }
    json.append(jsonSep + "[" + token.loc() + "," + entropy + "]");
    jsonSep = ",";
  }

  public void finish() {
    html.append("</pre></body></html>");
    json.append("]");
  }

  public String getHtml() {
    return html.toString();
  }

  public String getJson() {
    return json.toString();
  }
}
