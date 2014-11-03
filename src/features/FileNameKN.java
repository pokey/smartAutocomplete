package smartAutocomplete;

import java.util.*;

import fig.basic.*;
import static fig.basic.LogInfo.logs;

import smartAutocomplete.util.*;

public class FileNameKN extends FeatureDomain {
  private DataSummary summary;
  private FileTokenKNCounts fileTokenStatistic;

  public FileNameKN(Corpus corpus, Statistics statistics) {
    fileTokenStatistic =
      statistics.requireStatistic(FileTokenKNCounts.class, corpus);
    summary = fileTokenStatistic.getSummary();
  }

  private PredictionContext cachedContext = null;
  private boolean cachedShouldPredict = false;

  private boolean shouldPredict(PredictionContext context) {
    if (context != cachedContext) {
      cachedContext = context;
      cachedShouldPredict = fileTokenStatistic.useFileName(context);
    }

    return cachedShouldPredict;
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    if (!shouldPredict(context)) return;

    double prob =
      KneserNey.computeProb(CandidateNgram.get(context, candidate,
                                               fileTokenStatistic.getDecorator(context)),
                            summary);
    addFeature(candidate.features, "logProb", Math.log(prob));
  }

  @Override
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) {
    if (!shouldPredict(context)) return;

    Object info =
      KneserNey.getInfo(summary, CandidateNgram.get(context, candidate,
                                                    fileTokenStatistic.getDecorator(context)));
    putFeatureInfo(map, "logProb", info);
  }
}
