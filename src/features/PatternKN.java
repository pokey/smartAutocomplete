package smartAutocomplete;

import java.util.*;

import fig.basic.*;
import fig.exec.*;
import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

public class PatternKN extends FeatureDomain {
  private DataSummary summary;
  private PatternKNCounts patternStatistic;

  public PatternKN(Corpus corpus, Statistics statistics) {
    patternStatistic = 
      statistics.requireStatistic(PatternKNCounts.class, corpus);
    summary = patternStatistic.getSummary();
  }

  private int computeLastRare(PredictionContext context) {
    SubList<String> contextTokens = NgramContext.get(context).getTokens();
    for (int i=1; i<contextTokens.size()+1; i++) {
      if (patternStatistic.isRare(contextTokens.get(contextTokens.size()-i))) {
        return i;
      }
    }
    return -1;
  }

  private PredictionContext cachedContext = null;
  private String cachedAnnotation = null;

  private String annotation(PredictionContext context) {
    if (context != cachedContext) {
      cachedContext = context;
      int lastRare = computeLastRare(context);
      cachedAnnotation = ",lastRare=" + lastRare;
    }

    return cachedAnnotation;
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    double prob =
      KneserNey.computeProb(CandidateNgram.get(context, candidate, patternStatistic.getDecorator()),
                            summary);
    addFeature(candidate.features, "logProb" + annotation(context),
               Math.log(prob));
  }

  @Override
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) {
    Object info =
      KneserNey.getInfo(summary, CandidateNgram.get(context, candidate,
            patternStatistic.getDecorator()));
    putFeatureInfo(map, "logProb" + annotation(context), info);
  }
}
