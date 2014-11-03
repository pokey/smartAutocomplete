package smartAutocomplete;

import java.util.*;

import fig.basic.*;

import smartAutocomplete.util.*;

public class NgramContext {
  private final SubList<String> tokens;  // n-gram not including OOV tokens

  private int max_n;  // Size of n-gram to use.  Can vary depending on
                      // position in doc and presence of out-of-vocabulary
                      // words

  private static PredictionContext cachedContext = null;
  private static NgramContext cachedNgramContext = null;

  public static int getMax_n(SubList<Token> original) {
    int max_n = original.size()+1;

    for (int j = 0; j < original.size(); j++) {
      // Discard context if it contains out-of-vocabulary word
      if (Main.isFixedVocab() && original.get(j).type() == Token.Type.OOV) {
        max_n = original.size()-j;
      }
    }
    return max_n;
  }

  public static NgramContext get(PredictionContext context) {
    if (context != cachedContext) {
      SubList<Token> prefix = context.getPrefix();
      int max_n = Math.min(Main.ngramOrder, prefix.size()+1);
      cachedNgramContext =
        new NgramContext(prefix.subList(prefix.size()-max_n+1,
                                        prefix.size()));
    }

    return cachedNgramContext;
  }

  private NgramContext(SubList<Token> original) {
    List<String> full_ngram = new ArrayList<String>();
    max_n = getMax_n(original);
    for (int j = original.size()-max_n+1; j < original.size(); j++) {
      full_ngram.add(original.get(j).str());
    }

    tokens = new SubList<String>(full_ngram, 0, full_ngram.size());
  }

  SubList<String> getTokens() { return tokens; }
  int getMax_n() { return max_n; }

  // Returns words of context for given n-gram size.  For example, with n=1,
  // we get unigram context, which is always an empty string.
  SubList<String> subContext(int n) {
    return tokens.subList(max_n-n, max_n-1);
  }

  String contextStr() {
    return StrUtil.join(tokens.subList(0, max_n-1), " ");
  }
}
