package smartAutocomplete;

import fig.basic.*;

public class SimpleRecency extends FeatureDomain {
  @Option(gloss="How many tokens to look back") public static int window = 30;

  public SimpleRecency(Corpus corpus, Statistics statistics) {}

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<Token> prefix = context.getPrefix();

    // Last $window tokens
    for (int i=Math.max(prefix.end-window,0); i<prefix.end; i++) {
      if (prefix.get(i).str().equals(candidate.token)) {
        addFeature(candidate.features, "inLast" + window, 1);
        return;
      }
    }

    // In file
    for (int i=0; i<prefix.end-window; i++) {
      if (prefix.get(i).str().equals(candidate.token)) {
        addFeature(candidate.features, "inFile", 1);
        break;
      }
    }
  }
}
