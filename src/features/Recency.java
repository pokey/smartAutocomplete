package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class Recency extends FeatureDomain {
  @Option(gloss = "Number of tokens which should be considered common")
    public static int numTokens = 500;

  @Option(gloss="How many tokens to look back") public static int window = 30;

  private Rarity rarity;

  private static final String rareToken = "<rare>";

  public Recency(Corpus corpus, Statistics statistics) {
    rarity = new OnlineRare(corpus, statistics);
  }

  private String getRare(String token) {
    return rarity.isRare(token) ? rareToken : token;
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<Token> prefix = context.getPrefix();
    String token = "'" + getRare(candidate.token) + "'";

    // Last $window tokens
    for (int i=Math.max(prefix.end-window,0); i<prefix.end; i++) {
      if (prefix.get(i).str().equals(candidate.token)) {
        addFeature(candidate.features, "inLast" + window, 1);
        addFeature(candidate.features, token + ",inLast" + window, 1);
        return;
      }
    }

    // In file
    for (int i=0; i<prefix.end-window; i++) {
      if (prefix.get(i).str().equals(candidate.token)) {
        addFeature(candidate.features, "inFile", 1);
        addFeature(candidate.features, token + ",inFile", 1);
        break;
      }
    }

    // TODO: recency feature that measures distance (bucketed) rather than just crude
    // TODO: look at count of feature
  }
}
