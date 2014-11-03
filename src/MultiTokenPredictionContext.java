package smartAutocomplete;

import fig.basic.*;
import java.util.*;

/*
 * A prediction context where we have some hypothetical tokens of context.
 * Used for multi-token predictions during live editing.  
 */

class MultiTokenPredictionContext implements PredictionContext {
  private String path;
  private static List<Token> scratchTokens;
  private SubList<Token> prefix;
  private SubList<Token> suffix;
  private String base = null;
  private Document doc;

  private static SplitDocument prevDoc = null;
  private static MultiTokenPredictionContext current = null;

  MultiTokenPredictionContext(SplitDocument doc, MultiTokenCandidate prev,
                              String base) {
    this.doc = doc;
    this.base = base;
    this.prefix = makePrefix(prev, doc);
    this.suffix = new SubList<Token>(doc.tokens, doc.getSplitPos(),
                                     doc.tokens.size());
    current = this;
  }

  private static void setupScratchDoc(SplitDocument doc) {
    if (doc != prevDoc) {
      scratchTokens = new ArrayList<Token>();
      for (int i=0; i<doc.getSplitPos(); i++) {
        scratchTokens.add(doc.tokens.get(i));
      }
      prevDoc = doc;
    }
  }

  private static SubList<Token> makePrefix(MultiTokenCandidate prev,
                                           SplitDocument doc) {
    setupScratchDoc(doc);
    int pos = doc.getSplitPos();
    if (prev == null) {
      return new SubList<Token>(scratchTokens, 0, pos);
    } else {
      int length = pos + prev.getLength();
      int idx = length - 1;

      // Add to end of scratchTokens if it's too short
      if (scratchTokens.size() == idx) {
        scratchTokens.add(new Token(prev.getLast().token, -1));
        prev = prev.getPrev();
        idx--;
      }

      while (prev != null) {
        scratchTokens.set(idx, new Token(prev.getLast().token, -1));
        prev = prev.getPrev();
        idx--;
      }
      return new SubList<Token>(scratchTokens, 0, length);
    }
  }

  public SubList<Token> getPrefix() {
    if (current != this) {
      throw new RuntimeException("Used stale MultiTokenPredictionContext");
    }
    return prefix;
  }

  public SubList<Token> getSuffix() {
    return suffix;
  }

  public String getPath() {
    return doc.path;
  }

  public String getBase() {
    return base;
  }
}
