package smartAutocomplete;

import fig.basic.*;

public class LatentKN extends FeatureDomain {
  @Option(gloss="Use KN emulation parameters that give KN") public static boolean knEmKnParams = false;
  @Option(gloss="Value to use to shut off a cluster") public static double shutOff = Double.NEGATIVE_INFINITY;
  @Option(gloss="Use hand-crafted params") public static boolean initLLParams = false;

  private DataSummary summary;

  public LatentKN(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }


  private void putParam(Params params, int cluster, String feature, double val) {
    params.weights.put(toFeature(Main.clusterDecorators[cluster].decorate(feature)),
                       val);
  }

  private void addPoolFactor(Params params, int cluster, int k) {
    String kTotalStr = (k == Main.ngramOrder ? "total" : "backoffTotal") + k;
    putParam(params, cluster, "distinct" + k + ".log", 1);
    putParam(params, cluster, kTotalStr + "=0", -Math.log(KneserNey.discount(k, k == Main.ngramOrder)));
    putParam(params, cluster, kTotalStr + ".log", -1);
  }

  private void addKNEmParams(Params params, int n, boolean isBackoff,
                             int cluster1, int cluster2) {
    String countStr = (isBackoff ? "backoffCount" : "count") + n;
    String totalStr = (isBackoff ? "backoffTotal" : "total") + n;

    putParam(params, cluster1, countStr + ".log", 1);
    putParam(params, cluster1, countStr + "=0", shutOff);
    putParam(params, cluster1, totalStr + ".log", -1);
    putParam(params, cluster1, totalStr + "=0", shutOff);

    putParam(params, cluster2, "zero" + n, shutOff);
    putParam(params, cluster2, totalStr + ".log", -1);
    putParam(params, cluster2, totalStr + "=0", shutOff);

    double bias = 0;
    for (int k=n+1; k<=Main.ngramOrder; k++) {
      if (isBackoff) {
        bias += Math.log(KneserNey.discount(k, k == Main.ngramOrder));
        addPoolFactor(params, cluster1, k);
        addPoolFactor(params, cluster2, k);
      } else {
        String kTotalStr = "total" + k;
        putParam(params, cluster1, kTotalStr + "=1", shutOff);
        putParam(params, cluster1, kTotalStr + ".log", shutOff);

        putParam(params, cluster2, kTotalStr + "=1", shutOff);
        putParam(params, cluster2, kTotalStr + ".log", shutOff);
      }
    }
    putParam(params, cluster1, "bias", bias);
    putParam(params, cluster2, "bias", bias + Math.log(1-KneserNey.discount(n, !isBackoff)));
  }

  void addKNEmParams(Params params) {
    if (Main.clusters < Main.ngramOrder*4-2)
      throw new RuntimeException("Only " + Main.clusters + " clusters.  Need at least " +
                                 (Main.ngramOrder*4-2) + " clusters to emulate KN.");
    boolean topLevel = true;
    int cluster = 0;
    for (int n=Main.ngramOrder; n>=1; n--) {
      addKNEmParams(params, n, false, cluster++, cluster++);
      if (!topLevel)
        addKNEmParams(params, n, true, cluster++, cluster++);
      topLevel = false;
    }
  }

  @Override
  protected void initParams(Params params) {
    if (knEmKnParams) addKNEmParams(params);

    if (initLLParams) {
      putParam(params, 0, "count2.log", 1);
      putParam(params, 0, "total2.log", -1);

      putParam(params, 1, "count1.log", 1);
      putParam(params, 1, "total1.log", -1);
    }
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);

    addFeature(candidate.clusterFeatures, "bias", 1);
    addFeatures(summary, candidate.clusterFeatures, candidate_ngram, true);
  }

  private void addFeatures(DataSummary summary, FeatureVector features,
                           SubList<String> ngram, boolean topLevel) {
    int n = ngram.size();
    SubList<String> context_ngram = ngram.subList(0, n-1);  // Prefix
    SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix

    if (n == 0) return;  // Never seen this before!

    double total = summary.totalCounts[n-1].get(context_ngram, 0);
    double count = summary.counts[n].get(ngram, 0);

    addLogCountFeature(features, "count" + n,
                       Math.max(count - 1, 0));
    addLogCountFeature(features, "total" + n, total);
    addFeature(features, "zero" + n,
                 count == 0 ? 1 : 0);
    addLogCountFeature(features, "distinct" + n,
                       summary.distinctCounts[n-1].get(context_ngram, 0));
    if (!topLevel) {
      double backoffCount = summary.knCounts[n].get(ngram, 0);
      double backoffTotal = summary.knTotalCounts[n-1].get(context_ngram, 0);
      addLogCountFeature(features, "backoffCount" + n,
                   Math.max(backoffCount - 1, 0));
      addLogCountFeature(features, "backoffTotal" + n, backoffTotal);
    }
    // logs("%s: name = %s, count = %s, total = %s, factor = %s, prob = %s", ngram, name, count, total, factor, prob);
    addFeatures(summary, features, backoff_ngram, false);
  }

  // Add a variety of features relating to the number |count|.
  private void addLogCountFeature(FeatureVector features, String name, double count) {
    if (count == 0) addFeature(features, name + "=0", 1);
    else if (count == 1) addFeature(features, name + "=1", 1);
    else addFeature(features, name + ".log", LogTable.log((int)count));
  }
}
