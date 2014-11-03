package smartAutocomplete;

import java.util.*;

public class NgramKN extends FeatureDomain {
  private DataSummary summary;

  public NgramKN(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    double prob =
      KneserNey.computeProb(CandidateNgram.get(context, candidate), summary);
    addFeature(candidate.features, "logProb", Math.log(prob));
  }

  @Override
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) {
    Object info =
      KneserNey.getInfo(summary, CandidateNgram.get(context, candidate));
    putFeatureInfo(map, "logProb", info);
  }
}
