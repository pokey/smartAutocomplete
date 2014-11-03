package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class Document implements MemUsage.Instrumented {
  public final String path;
  public final List<Token> tokens;
  public int[] lineNums; // TODO: keep track of line numbers (1-based)
  boolean count = false;  // Whether to build up counts on this example
  boolean tune = false;  // Whether to tune discriminative weights on this example
  boolean eval = false;  // Whether to evaluate on this example
  boolean web = false;  // Whether to make this example available for display via http
  boolean lastCount = false;  // Whether this is the last document to count for given lang

  public Document(String path, List<Token> tokens) {
    this.path = path;
    this.tokens = tokens;
  }

  public long getBytes() {
    return MemUsage.objectSize(MemUsage.pointerSize * 3 + MemUsage.booleanSize * 5) +
           MemUsage.getBytes(path) +
           MemUsage.getBytes(tokens);
  }

  public SubList<Token> getPrefix(int pos) {
    return new SubList<Token>(tokens, 0, pos);
  }

  public int start() {
    return ReadData.singleBOS ? 1 : Main.ngramOrder - 1;
  }

  public int end() {
    return tokens.size();
  }

  public String getModeStr() {
    StringBuilder mode = new StringBuilder();
    String delim = "";
    if (count) delim = addMode(mode, delim, "count");
    if (tune) delim = addMode(mode, delim, "tune");
    if (eval) delim = addMode(mode, delim, "eval");

    return mode.toString();
  }

  private String addMode(StringBuilder mode, String delim, String str) {
    mode.append(delim + str);
    return " ";
  }
}
