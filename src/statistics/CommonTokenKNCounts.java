package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class CommonTokenKNCounts extends Statistic {
  @Option(gloss = "Number of tokens which should be considered common")
    public static int numTokens = 50;

  // N-grams with rare tokens replaced by rare token
  private DataSummary summary = new DataSummary();

  private Rarity rarity;

  private static final String rareToken = "<rare>";

  public CommonTokenKNCounts(Corpus corpus, Statistics statistics) {
    rarity = new FixedRare(numTokens);
  }

  public DataSummary getSummary() { return summary; }

  private TokenDecorator decorator = new TokenDecorator() {
    @Override
    public void decorate(ArrayList<String> tokens, Mode mode) {
      int start =
        mode == TokenDecorator.Mode.updateCandidate ? tokens.size()-1 : 0;
      for (int i=start; i<tokens.size(); i++) {
        if (rarity.isRare(tokens.get(i))) {
          tokens.set(i, rareToken);
        }
      }
    }
  };

  public boolean isRare(String token) {
    return rarity.isRare(token);
  }

  public TokenDecorator getDecorator() { return decorator; }

  @Override
  public void count(PredictionContext context, String val) {
    summary.count(CountingNgram.get(context, val, decorator));
  }

  @Override
  public void uncount(PredictionContext context, String val) {
    summary.uncount(CountingNgram.get(context, val, decorator));
  }
}
