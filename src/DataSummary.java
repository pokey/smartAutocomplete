package smartAutocomplete;

import java.util.*;
import java.io.*;

import com.google.common.collect.Lists;

import fig.basic.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

import smartAutocomplete.util.*;

public class DataSummary {
  // Statistics populated by counting
  TDoubleMap<SubList<String>>[] counts;  // n-gram => number of times it occurs in the corpus (x => N(x))
  double[][] countOfCounts;  // Number of ngrams of order $1 that appear $2+1 times
  TDoubleMap<SubList<String>>[] totalCounts;  // n-gram => sum of counts (x => sum_y count(xy))
  TDoubleMap<SubList<String>>[] distinctCounts;  // n-gram => number of tokens that follow it (x => {y : N(xy) > 0})
  TDoubleMap<SubList<String>>[] knCounts;  // n-gram => number of tokens that precede it (x => {y : N(yx) > 0})
  double[][] knCountOfCounts;  // Number of ngrams of order $1 that appear $2 times
  TDoubleMap<SubList<String>>[] knTotalCounts;  // n-gram => sum of knCounts (x => sum_y knCount(xy))
  int totalSeen = 0;

  // Cached discounts for use in kn
  boolean cachedDiscounts = false;
  double[] discounts;
  double[] knDiscounts;

  // Just to prune the space of candidates (really inefficient)
  HashMap<SubList<String>, Set<String>> candidateTokens = new HashMap<SubList<String>, Set<String>>();

  private Vocab vocab;

  public Vocab getVocab() { return vocab; }

  public int getVocabSize() { return vocab.getSize(); }

  public DataSummary() {
    vocab = Main.isFixedVocab() ? Main.getVocab() : new Vocab();

    // Initialize
    counts = new TDoubleMap[Main.ngramOrder+1];
    countOfCounts = new double[Main.ngramOrder][2];
    totalCounts = new TDoubleMap[Main.ngramOrder+1];
    distinctCounts = new TDoubleMap[Main.ngramOrder+1];
    knCounts = new TDoubleMap[Main.ngramOrder+1];
    knCountOfCounts = new double[Main.ngramOrder][2];
    knTotalCounts = new TDoubleMap[Main.ngramOrder+1];

    discounts = new double[Main.ngramOrder];
    knDiscounts = new double[Main.ngramOrder];

    for (int n = 0; n <= Main.ngramOrder; n++) {
      counts[n] = new TDoubleMap<SubList<String>>();
      totalCounts[n] = new TDoubleMap<SubList<String>>();
      distinctCounts[n] = new TDoubleMap<SubList<String>>();
      knCounts[n] = new TDoubleMap<SubList<String>>();
      knTotalCounts[n] = new TDoubleMap<SubList<String>>();
    }
  }

  private void incrementCountOfCounts(int n, double count, double[][] countOfCounts) {
    cachedDiscounts = false;
    if (count == 1) {
      countOfCounts[n-1][0]++;
    } else if (count == 2) {
      countOfCounts[n-1][0]--;
      countOfCounts[n-1][1]++;
    } else if (count == 3) {
      countOfCounts[n-1][1]--;
    }
  }

  private void decrementCountOfCounts(int n, double count, double[][] countOfCounts) {
    cachedDiscounts = false;
    if (count == 0) {
      countOfCounts[n-1][0]--;
    } else if (count == 1) {
      countOfCounts[n-1][0]++;
      countOfCounts[n-1][1]--;
    } else if (count == 2) {
      countOfCounts[n-1][1]++;
    }
  }

  private void computeDiscount(int n, boolean topLevel) {
    double[][] coc = topLevel ? countOfCounts : knCountOfCounts;
    double n1 = coc[n][0];
    double n2 = coc[n][1];
    // logs("n: %d, topLevel: %b, n1: %s, n2: %s", n+1, topLevel, Fmt.D(n1), Fmt.D(n2));
    if (n1 == 0 || n2 == 0) {
      logs("One of required kn count-of-counts for %d-grams is 0", n+1);
      return;
    }
    double[] d = topLevel ? discounts : knDiscounts;
    d[n] = n1 / (n1 + 2*n2);
  }

  public double discount(int n, boolean topLevel, double count) {
    if (!cachedDiscounts) {
      cachedDiscounts = true;
      for (int i = 0; i < Main.ngramOrder; i++) {
        computeDiscount(i, true);
        if (i < Main.ngramOrder - 1)
          computeDiscount(i, false);
      }
    }
    return topLevel ? discounts[n-1] : knDiscounts[n-1];
  }

  public void count(CountingNgram fullNgram) {
    this.count(fullNgram.getTokens(), fullNgram.getMax_n());
  }

  // Update all the language modeling counts.
  // Note: This would be more efficient if we processed tokens in batch
  // (could compute counts first and then compute the other statistics based
  // on counts), but we need things to be online.
  public void count(SubList<String> max_ngram, int max_n) {
    int size = max_ngram.size();
    if (!Main.isFixedVocab()) {
      String token = max_ngram.get(size-1);
      if (!vocab.contains(token)) {
        vocab.add(token);
      }
    } else if (!vocab.contains(max_ngram.get(size-1))) return;
    totalSeen++;

    // To match srilm, we need to have kn count at beginning just be normal
    // count
    if (max_ngram.start == 0) {
      double knCount = knCounts[size].incr(max_ngram, 1);
      incrementCountOfCounts(size, knCount, knCountOfCounts);
      if (size >= 1) {
        knTotalCounts[size-1].incr(max_ngram.subList(0, size-1), 1);
      }
    }

    for (int n = 0; n <= size; n++) {
      SubList<String> ngram = max_ngram.subList(size-n, size);
      SubList<String> context_ngram = ngram.subList(0, n-1);  // Prefix
      SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix
      double count = counts[n].incr(ngram, 1);
      boolean isNew = (count == 1);
      if (n >= 1) {
        incrementCountOfCounts(n, count, countOfCounts);
        // logs("ngram: %s, context_ngram: %s, backoff_ngram: %s", ngram, context_ngram, backoff_ngram);
        // To match behavior of srilm, we need to keep track of whether we've
        // seen <oov> token in order to update kn counts, but we don't want to
        // treat it as an actual token, so we shouldn't update total count
        if (!Main.isFixedVocab() || n <= max_n) {
          totalCounts[n-1].incr(context_ngram, 1);
        }
        if (isNew) {
          distinctCounts[n-1].incr(context_ngram, 1);
          double knCount = knCounts[n-1].incr(backoff_ngram, 1);
          if (n >= 2) {
            incrementCountOfCounts(n-1, knCount, knCountOfCounts);
            if (!Main.isFixedVocab() || n-1 <= max_n) {
              knTotalCounts[n-2].incr(backoff_ngram.subList(0, n-2), 1);
            }
          }
        }
      }
      //if (n >= 1) evaluation.addCumulative("#"+n+"grams", isNew);

      if (!Main.isFixedVocab() && n <= 3) {
        MapUtils.addToSet(candidateTokens, context_ngram, ngram.get(ngram.size() - 1));
      }
    }
  }

  public void uncount(CountingNgram fullNgram) {
    this.uncount(fullNgram.getTokens(), fullNgram.getMax_n());
  }

  // Undo counting an n-gram.
  public void uncount(SubList<String> max_ngram, int max_n) {
    int size = max_ngram.size();

    // To match srilm, we need to have kn count at beginning just be normal
    // count
    if (max_ngram.start == 0) {
      double knCount = knCounts[size].incr(max_ngram, -1);
      decrementCountOfCounts(size, knCount, knCountOfCounts);
      if (size >= 1) {
        knTotalCounts[size-1].incr(max_ngram.subList(0, size-1), -1);
      }
    }
    totalSeen--;

    boolean delete = false;
    for (int n = 0; n <= size; n++) {
      SubList<String> ngram = max_ngram.subList(size-n, size);
      SubList<String> context_ngram = ngram.subList(0, n-1);  // Prefix
      SubList<String> backoff_ngram = ngram.subList(1, n);  // Suffix
      double count = counts[n].incr(ngram, -1);
      boolean wasOnly = (count == 0);
      if (n >= 1) {
        decrementCountOfCounts(n, count, countOfCounts);
        // logs("ngram: %s, context_ngram: %s, backoff_ngram: %s", ngram, context_ngram, backoff_ngram);
        // To match behavior of srilm, we need to keep track of whether we've
        // seen <oov> token in order to update kn counts, but we don't want to
        // treat it as an actual token, so we shouldn't update total count
        if (!Main.isFixedVocab() || n <= max_n) {
          totalCounts[n-1].incr(context_ngram, -1);
        }
        if (wasOnly) {
          distinctCounts[n-1].incr(context_ngram, -1);
          double knCount = knCounts[n-1].incr(backoff_ngram, -1);
          if (n == 1) {
            delete = true;
          } else {
            decrementCountOfCounts(n-1, knCount, knCountOfCounts);
            if (!Main.isFixedVocab() || n-1 <= max_n) {
              knTotalCounts[n-2].incr(backoff_ngram.subList(0, n-2), -1);
            }
          }
        }
      }

      if (!Main.isFixedVocab() && n <= 3 && wasOnly) {
        MapUtils.removeFromSet(candidateTokens, context_ngram, ngram.get(ngram.size() - 1));
      }
    }
    if (!Main.isFixedVocab() && delete) {
      vocab.remove(max_ngram.get(size-1));
    }
  }

  void writeNgramTotals(String path, int n, boolean useKN) {
    writeCounts(path, useKN ? knTotalCounts[n-1] : totalCounts[n-1]);
  }

  void writeNgrams(String path, int n, boolean useKN) {
    writeCounts(path, useKN ? knCounts[n] : counts[n]);
  }

  private void writeCounts(String path, TDoubleMap<SubList<String>> count) {
    PrintWriter out = IOUtils.openOutHard(path);

    List<TDoubleMap<SubList<String>>.Entry> entries =
      Lists.newArrayList(count.entrySet());
    Collections.sort(entries, count.entryValueComparator());
    Collections.reverse(entries);
    double cumulative = 0;
    for (TDoubleMap<SubList<String>>.Entry entry : entries) {
      double value = entry.getValue();
      cumulative += value;
      if (value > 2.9999) {
        out.println(StrUtil.join(entry.getKey(), " ") + "\t" + value + "\t" + cumulative);
      }
    }
    out.println("cumulative\t0\t" + cumulative);

    out.close();
  }

  void dumpInfo() {
    begin_track("dumpInfo");
    for (int n = 0; n <= Main.ngramOrder; n++) {
      if (counts[n].size() > 0) logs("counts[%d]: %s", n, counts[n]);
      if (totalCounts[n].size() > 0) logs("totalCounts[%d]: %s", n, totalCounts[n]);
      if (distinctCounts[n].size() > 0) logs("distinctCounts[%d]: %s", n, distinctCounts[n]);
      if (knCounts[n].size() > 0) logs("knCounts[%d]: %s", n, knCounts[n]);
      if (knTotalCounts[n].size() > 0) logs("knTotalCounts[%d]: %s", n, knTotalCounts[n]);
    }
    end_track();
  }
}
