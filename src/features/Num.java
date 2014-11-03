package smartAutocomplete;

import fig.basic.*;

class Num extends FeatureDomain {
  private DataSummary summary;

  public Num(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);

    int max_n = candidate_ngram.size();
    for (int n = 1; n <= max_n; n++) {
      SubList<String> backoff_ngram =
        candidate_ngram.subList(max_n-n, max_n);
      addCountFeature("count"+n, summary.counts[n].get(backoff_ngram, 0),
                      candidate.features);
      addCountFeature("knCount"+n, summary.knCounts[n].get(backoff_ngram, 0),
                      candidate.features);
    }
  }

  // Add a variety of features relating to the number |count|.
  private void addCountFeature(String name, double count, FeatureVector features) {
    if (count == 0) addFeature(features, name + "=0", 1);
    else if (count == 1) addFeature(features, name + "=1", 1);
    else if (count == 2) addFeature(features, name + "=2", 1);
    else addFeature(features, name + ">=3", 1);
    addFeature(features, name + ".log", Math.log(count+1));
  }
}
