package smartAutocomplete.util;

import java.util.*;
import java.util.regex.*;
import java.io.*;

public class Template {
  static private Pattern tokenizerRegex =
    Pattern.compile("\\{\\{(\\w+)\\}\\}");

  private List<String> sections = new ArrayList<String>();
  private List<String> names = new ArrayList<String>();

  public Template(String path) throws IOException {
    String text = FileUtil.readFile(path);
    Matcher match = tokenizerRegex.matcher(text);
    int lastLoc = 0;
    while (match.find()) {
      int start = match.start();
      sections.add(text.substring(lastLoc, start));
      names.add(match.group(1));
      lastLoc = start + match.group(0).length();
    }
    sections.add(text.substring(lastLoc));
  }

  public String instantiate(HashMap<String, String> context) {
    StringBuilder ret = new StringBuilder();

    for (int i=0; i<names.size(); i++) {
      ret.append(sections.get(i));
      ret.append(context.get(names.get(i)));
    }
    ret.append(sections.get(sections.size()-1));

    return ret.toString();
  }
}
