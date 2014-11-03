package smartAutocomplete;

import java.util.*;

import fig.basic.*;

import edu.berkeley.nlp.lm.*;
import edu.berkeley.nlp.lm.ContextEncodedNgramLanguageModel.LmContextInfo;

public class BerkeleyNgram extends FeatureDomain {
  private BerkeleyLM statistic = null;
  private PredictionContext cachedContext = null;
  private LmContextInfo cachedBerkeleyContext = null;

  public BerkeleyNgram(Corpus corpus, Statistics statistics) {
    statistic =
      statistics.requireStatistic(BerkeleyLM.class, corpus);
  }

  @Override
  void addFeatures(PredictionContext context, Candidate candidate) {
    ContextEncodedNgramLanguageModel<String> lm = statistic.getLm();
    final WordIndexer<String> wordIndexer = lm.getWordIndexer();
    if (context != cachedContext) {
      cachedContext = context;
      cachedBerkeleyContext = new LmContextInfo();
      NgramContext ngramContext = NgramContext.get(context);
      SubList<String> tokens = ngramContext.getTokens();
      for (int i = 0; i < tokens.size(); i++) {
        lm.getLogProb(cachedBerkeleyContext.offset,
            cachedBerkeleyContext.order,
            wordIndexer.getIndexPossiblyUnk(tokens.get(i)),
            cachedBerkeleyContext);
      }
    }
    double prob =
      lm.getLogProb(cachedBerkeleyContext.offset,
          cachedBerkeleyContext.order,
          wordIndexer.getIndexPossiblyUnk(candidate.token), null);
    addFeature(candidate.features, "logProb", prob);
  }

  @Override
  void putInfo(Map<String, Object> map, PredictionContext context,
               Candidate candidate) {
    // FixMe: [usability] Add kn info
  }
}
