package smartAutocomplete;

import fig.basic.*;

class Num2 extends FeatureDomain {
  private DataSummary summary;

  public Num2(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);

    // Count features to emulate backoff KN.
    // The idea is there exists a setting of the weights that gives backoff KN back exactly.
    // By tuning the weights, we should do at least as well.
    // TODO: subtract discount (important?)
    // TODO: this doesn't work yet - make this work
    int max_n = candidate_ngram.size();
    FeatureVector features = candidate.features;
    for (int n = max_n; n >= 1; n--) {
      SubList<String> backoff_ngram =
        candidate_ngram.subList(max_n-n, max_n);
      double count = summary.counts[n].get(backoff_ngram, 0);
      addCountFeature("count"+n, count, features);
      if (n < max_n)
        addCountFeature("distinctCount"+n,
            summary.distinctCounts[n].get(candidate_ngram.subList(max_n-n-1,
                max_n-1), 0), features);
      addCountFeature("knCount"+n, summary.knCounts[n].get(backoff_ngram, 0), features);
      if (count > 0) break;  // Don't include lower order
    }
  }

  // Add a variety of features relating to the number |count|.
  private void addCountFeature(String name, double count,
                               FeatureVector features) {
    if (count == 0) addFeature(features, name + "=0", 1);
    else if (count == 1) addFeature(features, name + "=1", 1);
    else if (count == 2) addFeature(features, name + "=2", 1);
    else addFeature(features, name + ">=3", 1);
    addFeature(features, name + ".log", Math.log(count+1));
  }
}
