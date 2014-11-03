package smartAutocomplete;

import fig.basic.*;

/*
 * A prediction context where we are simulating the action of a user typing a
 * document from start to finish.  We try to predict the token at pos when the
 * user hasn't typed any base string.
 */
class SequentialContext implements PredictionContext {
  private String path;
  private SubList<Token> prefix;
  private SubList<Token> suffix;

  SequentialContext(Document doc, int pos) {
    this.path = doc.path;
    this.prefix = doc.getPrefix(pos);

    // Empty suffix; we pretend they're creating document sequentially
    this.suffix = new SubList<Token>(doc.tokens, pos, pos);
  }

  public SubList<Token> getPrefix() {
    return prefix;
  }

  public SubList<Token> getSuffix() {
    return suffix;
  }

  public String getPath() {
    return path;
  }

  public String getBase() {
    return "";
  }
}
