package smartAutocomplete;

import fig.basic.*;

// Emulate Backoff KN
class BackoffKNEm extends FeatureDomain {
  private DataSummary summary;

  public BackoffKNEm(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);
    int max_n = candidate_ngram.size();
    addFeatures(summary, candidate.features, candidate_ngram,
                max_n == Main.ngramOrder, max_n);
  }

  private void addFeatures(DataSummary summary, FeatureVector features,
                           SubList<String> ngram, boolean topLevel, int start) {
    int n = ngram.size();
    SubList<String> context_ngram = ngram.subList(0, n-1);  // Prefix
    SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix

    if (n == 0) return;  // Never seen this before!

    TDoubleMap<SubList<String>>[] totalCounts =
      topLevel ? summary.totalCounts : summary.knTotalCounts;
    double total = totalCounts[n-1].get(context_ngram, 0);

    if (total > 0) {
      TDoubleMap<SubList<String>>[] counts =
        topLevel ? summary.counts : summary.knCounts;
      double count = counts[n].get(ngram, 0);

      if (count > 0) {
        addFeature(features, "indicator" + n + ",start=" + start, 1);
        addFeature(features, "count" + n + ",start=" + start,
                   Math.log(count-KneserNey.discount(summary, topLevel, n,
                                                     count)));
        addFeature(features, "total" + n + ",start=" + start, Math.log(total));
        // logs("%s: name = %s, count = %s, total = %s, factor = %s, prob = %s", ngram, name, count, total, factor, prob);
      } else {
        addFeature(features, "distinct" + n + ",start=" + start,
                     Math.log(summary.distinctCounts[n-1].get(context_ngram, 0)));
        addFeature(features, "distinctTotal" + n + ",start=" + start, Math.log(total));
        addFeatures(summary, features, backoff_ngram, false, start);
      }
    } else {
      start = start - 1;
      addFeatures(summary, features, backoff_ngram, false, start);
    }
  }
}
