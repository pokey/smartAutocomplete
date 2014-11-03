package smartAutocomplete;

import java.util.*;

// Allows features to modify n-grams, eg replacing rare tokens by a special
// <rare> token
public interface TokenDecorator {
  public enum Mode {count, initCandidate, updateCandidate}
  // Modify tokens to change the n-gram.  If onlyLast is set, then only the
  // last token needs to be changed
  public void decorate(ArrayList<String> tokens, Mode mode);
}
