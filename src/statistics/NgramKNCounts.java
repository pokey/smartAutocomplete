package smartAutocomplete;

import fig.exec.*;

public class NgramKNCounts extends Statistic {
  private DataSummary summary = new DataSummary();
  public DataSummary getSummary() { return summary; }

  public NgramKNCounts(Corpus corpus, Statistics statistics) {}

  @Override
  public void count(PredictionContext context, String val) {
    summary.count(CountingNgram.get(context, val));
  }

  @Override
  public void uncount(PredictionContext context, String val) {
    summary.uncount(CountingNgram.get(context, val));
  }

  @Override
  public void write(String prefix) {
    int maxNgramSize = 1;
    for (int i=1; i<=maxNgramSize; i++) {
      summary.writeNgrams(Execution.getFile(prefix + ".counts" + i), i, false);
      summary.writeNgrams(Execution.getFile(prefix + ".diversityCounts" + i), i,
                          true);
      summary.writeNgramTotals(Execution.getFile(prefix + ".diversityTotals" + i),
                               i+1, true);
    }
  }
}
