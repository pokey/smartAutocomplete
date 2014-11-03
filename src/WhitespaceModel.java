package smartAutocomplete;

public class WhitespaceModel {
  public String toString(MultiTokenCandidate candidate) {
    String[] tokens = new String[candidate.getLength()];
    int idx = candidate.getLength()-1;
    while (candidate != null) {
      tokens[idx] = candidate.getLast().token;
      candidate = candidate.getPrev();
      idx--;
    }

    StringBuilder ret = new StringBuilder();
    String prev = null;
    for (String token : tokens) {
      if (prev != null) {
        if (Tokenizer.isIdentifier(prev) && Tokenizer.isIdentifier(token)) {
          ret.append(" ");
        }
        if ((prev.equals("if") || prev.equals("for") || prev.equals("while"))
            && token.equals("(")) {
          ret.append(" ");
        }
        if (token.equals("{")) ret.append(" ");
        // if (prev.equals("{")) ret.append("\n");
        if (prev.equals("{")) ret.append(" ");
        if (prev.equals("=")) ret.append(" ");
        if (token.equals("=")) ret.append(" ");
        if (prev.equals(":")) ret.append(" ");
        if (token.equals(":")) ret.append(" ");
        if (prev.equals(",")) ret.append(" ");
        // if (prev.equals(";")) ret.append("\n");
        if (prev.equals(";")) ret.append(" ");
      }
      ret.append(token);
      prev = token;
    }
    if (prev.equals(";")) {
      // ret.append("\n");
      // ret.append(" ");
    }

    return ret.toString();
  }
}
