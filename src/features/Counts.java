package smartAutocomplete;

import fig.basic.*;

class Counts extends FeatureDomain {
  private DataSummary summary;

  public Counts(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);
    addFeatures(summary, candidate.features, candidate_ngram,
                true);
  }

  private void addFeatures(DataSummary summary, FeatureVector features,
                           SubList<String> ngram, boolean topLevel) {
    int n = ngram.size();
    SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix

    if (n == 0) return;  // Never seen this before!

    addLogCountFeature(features, "count" + n, summary.counts[n].get(ngram, 0));
    if (!topLevel) {
      addLogCountFeature(features, "diversityCount" + n,
                         summary.knCounts[n].get(ngram, 0));
    }

    addFeatures(summary, features, backoff_ngram, false);
  }

  // Add a variety of features relating to the number |count|.
  private void addLogCountFeature(FeatureVector features, String name, double count) {
    if (count == 0) addFeature(features, name + "=0", 1);
    else if (count == 1) addFeature(features, name + "=1", 1);
    else if (count == 2) addFeature(features, name + "=2", 1);
    else addFeature(features, name + ".log", LogTable.log((int)count));
  }
}
