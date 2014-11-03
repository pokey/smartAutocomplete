package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class OnlineRare implements Rarity {
  @Option(gloss = "Threshold count below which tokens considered rare")
    public static int threshold = 6;

  // Standard n-grams
  private DataSummary ngramSummary;

  public OnlineRare(Corpus corpus, Statistics statistics) {
    ngramSummary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  public boolean isRare(String token) {
    if (token.equals(Vocab.BEGIN)) return false;
    List<String> tokenNgram = new ArrayList<String>();
    tokenNgram.add(token);
    double count =
      ngramSummary.knTotalCounts[1].get(new SubList<String>(tokenNgram, 0,
                                                          tokenNgram.size()), 0);
    return count < ((double) threshold) - .0001;
  }
}
