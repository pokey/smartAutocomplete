package smartAutocomplete.httpServer;

import java.io.*;
import java.util.*;
import java.nio.*;
import java.nio.charset.*;
import java.nio.file.*;

import net.htmlescape.HtmlEscape;

import static fig.basic.LogInfo.logs;

import smartAutocomplete.*;
import smartAutocomplete.util.*;

public class CodeHtml {
  private static final Template template = createTemplate("webapp/code.html");
  private static Template createTemplate(final String path) {
    try {
      return new Template(path);
    } catch (final IOException exc) {
      throw new Error(exc);
    }  
  }

  private static HashMap<String, String> cachedHtml = new HashMap<String, String>();

  public static String getHtml(String path) {
    String html = null;
    if(!cachedHtml.containsKey(path)) {
      html = genHtml(path);
      cachedHtml.put(path, html);
    } else {
      html = cachedHtml.get(path);
    }
    return html;
  }

  private static String genHtml(String path) {
    String text = null;
    try {
      // FixMe: [performance] Don't repeat reading file
      byte[] encoded = Files.readAllBytes(Paths.get(path));
      text = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    List<Token> tokens = Tokenizer.tokenize(text, path);

    StringBuilder code = new StringBuilder();
    int lastLoc = 0;
    for (Token token : tokens) {
      if (token.type() != Token.Type.ACTUAL) continue;
      code.append(text.substring(lastLoc, token.loc()));
      lastLoc = token.loc() + token.str().length();
      code.append("<span id='zt" + token.loc() + "'>" + HtmlEscape.escape(token.str()) + 
                  "</span>");
    }

    HashMap<String, String> context = new HashMap<String, String>();
    context.put("path", path);
    context.put("code", code.toString());

    return template.instantiate(context);
  }
}
