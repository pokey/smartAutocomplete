package smartAutocomplete;

import java.util.*;

import fig.basic.*;
import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

public class KneserNey {
  @Option(gloss="Discounts for Kneser-Ney") public static List<Double> discounts = new ArrayList<Double>();
  @Option(gloss="Use Kneser Ney diversity counts") public static boolean useKN = true;

  private static boolean logKN = false;

  // Set whether or not to log during computing probability
  public static void logKNIs(boolean val) {
    logKN = val;
  }

  static public double computeProb(SubList<String> ngram,
                                   DataSummary summary) {
    return computeProb(ngram, summary, summary, ngram.size() == Main.ngramOrder);
  }

  // Use statsSummary to compute global discount parameters and number of
  // words in vocabulary.
  static public double computeProb(SubList<String> ngram,
                                   DataSummary statsSummary,
                                   DataSummary countsSummary) {
    return computeProb(ngram, statsSummary, countsSummary, ngram.size() == Main.ngramOrder);
  }

  static double discount(int n, boolean topLevel) {
    return discount(null, topLevel, n, -1);
  }

  static double discount(DataSummary summary, boolean topLevel, int n,
                         double count) {
    if (summary == null || discounts.size() > 0) {
      return discounts.size() == Main.ngramOrder ? discounts.get(n-1)
                                                 : discounts.get(0);
    } else {
      return summary.discount(n, topLevel, count);
    }
  }

  static public Object getInfo(DataSummary summary,
                               SubList<String> candidate_ngram) {
    List<Object> ret = new ArrayList<Object>();
    
    boolean topLevel = true;
    int n = candidate_ngram.size();
    for (int i=0; i<n; i++) {
      TDoubleMap<SubList<String>>[] counts =
        topLevel ? summary.counts : summary.knCounts;
      TDoubleMap<SubList<String>>[] totalCounts =
        topLevel ? summary.totalCounts : summary.knTotalCounts;
      double count = counts[n-i].get(candidate_ngram.subList(i, n), 0);
      double totalCount =
        totalCounts[n-1-i].get(candidate_ngram.subList(i, n-1), 0);
      ret.add(new Object[]{StrUtil.join(candidate_ngram.subList(i, n), " "),
                           count, totalCount});
      topLevel = false;
    }

    return ret;
  }

  static public double
  computeProb(SubList<String> ngram, DataSummary statsSummary,
              DataSummary countsSummary, boolean topLevel) {
    int n = ngram.size();
    SubList<String> context_ngram = ngram.subList(0, n-1);  // Prefix
    SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix

    if (n == 0) return 1/(double)statsSummary.getVocabSize();  // Never seen this before!

    TDoubleMap<SubList<String>>[] totalCounts =
      topLevel ? countsSummary.totalCounts : countsSummary.knTotalCounts;
    TDoubleMap<SubList<String>>[] counts =
      topLevel ? countsSummary.counts : countsSummary.knCounts;

    double total = totalCounts[n-1].get(context_ngram, 0);

    double lowOrderProb = computeProb(backoff_ngram, statsSummary, countsSummary,
                                      !useKN);

    if (total == 0) return lowOrderProb;  // Context doesn't exist at all

    double count = counts[n].get(ngram, 0);
    double discount = KneserNey.discount(statsSummary, topLevel, n, count);
    double main = Math.max(count - discount, 0);

    double distinct = countsSummary.distinctCounts[n-1].get(context_ngram, 0);
    double aux = discount * distinct * lowOrderProb;
    double prob = (main + aux) / total;
    assert !Double.isNaN(prob);

    if (logKN)
      logs("%s: topLevel=%s, count = %s, main = %s, total = %s, distinct = %s, aux = %s, result = %s", ngram, topLevel, count, main, total, distinct, aux, prob);
    return prob;
  }
}

