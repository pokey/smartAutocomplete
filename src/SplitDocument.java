package smartAutocomplete;

import java.util.*;

import fig.basic.*;

/*
 * The document is not complete; the user has started typing something at
 * splitPos, but we don't know what it will be.
 */
public class SplitDocument extends Document {
  private int splitPos;

  public int getSplitPos() { return splitPos; }

  public SplitDocument(String path, List<Token> tokens, int splitPos) {
    super(path, tokens);
    this.splitPos = splitPos;
  }

  @Override
  public SubList<Token> getPrefix(int pos) {
    int start = pos < splitPos ? 0 : splitPos;
    return new SubList<Token>(tokens, start, pos);
  }
}

