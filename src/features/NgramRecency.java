package smartAutocomplete;

import fig.basic.*;

public class NgramRecency extends FeatureDomain {
  private DataSummary summary;

  public NgramRecency(Corpus corpus, Statistics statistics) {}

  @Option(gloss="How many locations get their own weight before using log")
    public static int individualWeights = 10;

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    FeatureVector features = candidate.features;
    boolean found = false;
    SubList<Token> prefix = context.getPrefix();
    for (int i=prefix.end-1; i>=0; i--) {
      if (prefix.getSafe(i).str().equals(candidate.token)) {
        int dist = prefix.end - i;
        if (dist<=individualWeights) {
          addFeature(features, "last" + dist, 1);
        } else {
          addFeature(features, "last.log", Math.log(dist));
        }
        if (i > 0 &&
            prefix.getSafe(i-1).str().equals(prefix.getSafe(prefix.end-1).str())) {
          addFeature(features, "lastBigram", 1);
        }
        found = true;
        break;
      }
    }
    if (!found) addFeature(features, "notInFile", 1);
  }
}
