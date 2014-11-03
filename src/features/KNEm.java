package smartAutocomplete;

import fig.basic.*;

// Emulate interpolated KN with multiple discount parameters, as in modified
// KN.  Note this is the old version that doesn't actually emulate KN
public class KNEm extends FeatureDomain {
  @Option(gloss="Discount paramaters for KN emulation") public static double discountParams = 1;
  private DataSummary summary;

  public KNEm(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);

    int max_n = candidate_ngram.size();
    addFeatures(summary, candidate.features, candidate_ngram,
                max_n == Main.ngramOrder, 1, max_n);
  }

  private void addFeatures(DataSummary summary, FeatureVector features,
                           SubList<String> ngram, boolean topLevel,
                           double factor, int start) {
    int n = ngram.size();
    SubList<String> context_ngram = ngram.subList(0, n-1);  // Prefix
    SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix

    if (n == 0) return;  // Never seen this before!

    TDoubleMap<SubList<String>>[] totalCounts =
      topLevel ? summary.totalCounts : summary.knTotalCounts;
    double total = totalCounts[n-1].get(context_ngram, 0);

    double backoffFactor;
    if (total > 0) {
      TDoubleMap<SubList<String>>[] counts =
        topLevel ? summary.counts : summary.knCounts;
      double count = counts[n].get(ngram, 0);

      addFeature(features, "prob" + n + ",start=" + start, factor * count / total);
      if (count >= discountParams) {
        addFeature(features, "discount" + n + ">=" + discountParams +
            ",start=" + start, factor / total);
      } else {
        for (int i=1; i<discountParams; i++) {
          if (count == i) {
            addFeature(features, "discount" + n + "=" + i + ",start=" +
                start, factor / total);
            break;
          }
        }
      }
      // logs("%s: name = %s, count = %s, total = %s, factor = %s, prob = %s", ngram, name, count, total, factor, prob);

      backoffFactor = factor * summary.distinctCounts[n-1].get(context_ngram, 0)
                      / total;
    } else {
      backoffFactor = factor;
      start = start - 1;
    }

    addFeatures(summary, features, backoff_ngram, false, backoffFactor,
                start);
  }
}
