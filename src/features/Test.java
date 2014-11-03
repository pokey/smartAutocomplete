package smartAutocomplete;

import fig.basic.*;

public class Test extends FeatureDomain {
  @Option(gloss="Seed parameters") public static boolean seedParams = false;

  public Test(Corpus corpus, Statistics statistics) { }

  private void putParam(Params params, int cluster, String feature, double val) {
    params.weights.put(toFeature(Main.clusterDecorators[cluster].decorate(feature)),
                       val);
  }

  @Override
  protected void initParams(Params params) {
    if (seedParams) putParams(params);
  }

  void putParams(Params params) {
    putParam(params, 0, "count2.log", 1);
    putParam(params, 0, "total2.log", -1);

    putParam(params, 1, "count1.log", 1);
    putParam(params, 1, "total1.log", -1);
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);

    FeatureVector features = candidate.clusterFeatures;
    String contextWord = candidate_ngram.get(0);
    if (candidate.token == "w0")
      addFeature(features, "count1.log", 1);
    else if (candidate.token == "w1") {
      if (contextWord == "a")
        addFeature(features, "count2.log", 1);
      else // b
        addFeature(features, "count2.log", 10);
    }
    addFeature(features, "total1.log", 1);
    if (contextWord == "a")
      addFeature(features, "total2.log", 1);
    else // b
      addFeature(features, "total2.log", 10);
  }
}
