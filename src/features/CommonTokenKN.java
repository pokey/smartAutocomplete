package smartAutocomplete;

import java.util.*;

import fig.basic.*;
import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

public class CommonTokenKN extends FeatureDomain {
  private DataSummary summary;
  private CommonTokenKNCounts statistic;

  public CommonTokenKN(Corpus corpus, Statistics statistics) {
    statistic = 
      statistics.requireStatistic(CommonTokenKNCounts.class, corpus);
    summary = statistic.getSummary();
  }

  // Return the last position where there is a rare token, or -1 if there is
  // none
  private int computeLastRare(PredictionContext context) {
    SubList<String> contextTokens = NgramContext.get(context).getTokens();
    for (int i=1; i<contextTokens.size()+1; i++) {
      if (statistic.isRare(contextTokens.get(contextTokens.size()-i))) return i;
    }
    return -1;
  }

  private PredictionContext cachedContext = null;
  private int cachedLastRare = 0;

  private int lastRare(PredictionContext context) {
    if (context != cachedContext) {
      cachedContext = context;
      cachedLastRare = computeLastRare(context);
    }

    return cachedLastRare;
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    double prob =
      KneserNey.computeProb(CandidateNgram.get(context, candidate,
                                               statistic.getDecorator()),
                            summary);
    addFeature(candidate.features, "logProb,lastRare=" + lastRare(context),
               Math.log(prob));
  }

  @Override
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) {
    Object info =
      KneserNey.getInfo(summary, CandidateNgram.get(context, candidate,
                                                    statistic.getDecorator()));
    putFeatureInfo(map, "logProb,lastRare=" + lastRare(context), info);
  }
}
