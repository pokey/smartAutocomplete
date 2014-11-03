package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class NgramKNIndicators extends FeatureDomain {
  private DataSummary summary;

  public NgramKNIndicators(Corpus corpus, Statistics statistics) {
    summary =
      statistics.requireStatistic(NgramKNCounts.class, corpus).getSummary();
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);
    String annotation = CountKNFeatures.annotation(summary, candidate_ngram);

    double prob = KneserNey.computeProb(candidate_ngram, summary);
    addFeature(candidate.features, "logProb" + annotation, Math.log(prob));
  }

  @Override
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) {
    SubList<String> candidate_ngram = CandidateNgram.get(context, candidate);
    String annotation = CountKNFeatures.annotation(summary, candidate_ngram);

    Object info = KneserNey.getInfo(summary, candidate_ngram);
    putFeatureInfo(map, "logProb" + annotation, info);
  }
}
