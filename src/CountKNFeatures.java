package smartAutocomplete;

import fig.basic.*;

class CountKNFeatures {
  static final double[] bins = {0, 1, 2, 3, 4, 5, 10, 20, 30, 50, 100, 500, 1000};

  static int nonZero(DataSummary summary, SubList<String> candidate_ngram) {
    boolean topLevel = true;
    int n = candidate_ngram.size();
    for (int i=0; i<n-1; i++) {
      TDoubleMap<SubList<String>>[] totalCounts =
        topLevel ? summary.totalCounts : summary.knTotalCounts;
      double totalCount =
        totalCounts[n-1-i].get(candidate_ngram.subList(i, n-1), 0);
      if (totalCount > .0001) return n-i;
      topLevel = false;
    }
    return 1;
  }

  static String annotation(DataSummary countsSummary,
                           SubList<String> candidate_ngram) {
    boolean topLevel = true;
    int n = candidate_ngram.size();
    for (int i=0; i<n-1; i++) {
      TDoubleMap<SubList<String>>[] totalCounts =
        topLevel ? countsSummary.totalCounts : countsSummary.knTotalCounts;
      double totalCount =
        totalCounts[n-1-i].get(candidate_ngram.subList(i, n-1), 0);
      topLevel = false;
      if (totalCount > .0001) {
        // TDoubleMap<SubList<String>>[] counts =
        //   topLevel ? countsSummary.counts : countsSummary.knCounts;
        // double count = counts[n-i].get(candidate_ngram.subList(i, n), 0);
        // addFeatures(domain, features, logProb, count, n-i, "count");
        return annotation(totalCount, n-1-i, "total");
      }
    }
    return ",empty";
  }

  static private String annotation(double count, int n, String name) {
    for (int j=0; j<bins.length; j++) {
      if (count < bins[j]+.0001) {
        return "," + name + n + "<=" + bins[j];
      }
    }
    return "," + name + n + ">" + bins[bins.length-1];
  }
}
