package smartAutocomplete;

import java.util.*;

import fig.basic.*;

import smartAutocomplete.util.*;

/**
 * Ngram to use for incrementing KN counts. This includes oov tokens in the
 * context, because this ensures we correctly do kn-style counts.
 */
public class CountingNgram {
  private final SubList<String> tokens;   // n-gram including OOV's

  private int max_n;  // Size of n-gram to use.  Can vary depending on
                      // position in doc and presence of out-of-vocabulary
                      // words

  private static class CachedBuffer {
    private PredictionContext cachedContext = null;
    private String cachedVal = null;
    private CountingNgram cachedNgram = null;
    private TokenDecorator decorator;

    public CachedBuffer(TokenDecorator decorator) {
      this.decorator = decorator;
    }

    public CountingNgram get(PredictionContext context, String val) {
      if (context != cachedContext || val != cachedVal) {
        cachedContext = context;
        cachedVal = val;
        cachedNgram = new CountingNgram(context, val, decorator);
      }

      return cachedNgram;
    }
  }

  private static Map<TokenDecorator, CachedBuffer> cachedBuffers =
    new HashMap<TokenDecorator, CachedBuffer>();

  public static CountingNgram get(PredictionContext context, String val) {
    return get(context, val, IdentityTokenDecorator.getSingleton());
  }

  public static CountingNgram get(PredictionContext context, String val,
                                  TokenDecorator decorator) {
    CachedBuffer cachedBuffer;
    if (!cachedBuffers.containsKey(decorator)) {
      cachedBuffer = new CachedBuffer(decorator);
      cachedBuffers.put(decorator, cachedBuffer);
    } else {
      cachedBuffer = cachedBuffers.get(decorator);
    }

    return cachedBuffer.get(context, val);
  }

  private CountingNgram(PredictionContext context, String val,
                        TokenDecorator decorator) {
    SubList<Token> prefix = context.getPrefix();
    int max_n = Math.min(Main.ngramOrder, prefix.size()+1);
    SubList<Token> originalContext =
      prefix.subList(prefix.size()-max_n+1, prefix.size());
    this.max_n = NgramContext.getMax_n(originalContext);

    ArrayList<String> full_ngram = new ArrayList<String>();
    for (int j = 0; j < originalContext.size(); j++) {
      full_ngram.add(originalContext.get(j).str());
    }
    full_ngram.add(val);
    decorator.decorate(full_ngram, TokenDecorator.Mode.count);

    this.tokens = new SubList<String>(full_ngram, 0, full_ngram.size());
  }

  SubList<String> getTokens() { return tokens; }
  int getMax_n() { return max_n; }
}
