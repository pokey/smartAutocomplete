package smartAutocomplete;

import fig.basic.*;

public class Test2 extends FeatureDomain {
  @Option(gloss="Seed parameters") public static boolean seedParams = false;

  public Test2(Corpus corpus, Statistics statistics) {}


  private void putParam(Params params, int cluster, String feature, double val) {
    params.weights.put(toFeature(Main.clusterDecorators[cluster].decorate(feature)),
                       val);
  }

  @Override
  protected void initParams(Params params) {
    if (seedParams) putParams(params);
  }

  void putParams(Params params) {
    putParam(params, 0, "feat", 10);
    putParam(params, 1, "feat", -10);
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);

    FeatureVector features = candidate.clusterFeatures;
    // addFeature(features, "bias", 100);
    if (candidate.token == "w0") {
      if (candidate_ngram.get(0) == "a")
        addFeature(features, "feat", 1);
      else // b
        addFeature(features, "feat", -1);
    }
  }
}
