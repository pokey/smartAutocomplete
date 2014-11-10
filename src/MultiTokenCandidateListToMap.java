package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class MultiTokenCandidateListToMap {
  @Option(gloss = "Number of multitoken candidates to display.")
    public static int numMultiCandidates = 1;
  @Option(gloss = "Number of candidates to display.")
    public static int numCandidates = 3;

  public static Map<String, Object>
  toMap(MultiTokenCandidateList candidateList, int loc,
        WhitespaceModel whitespaceModel) {
    Map<String, Object> map = new HashMap<String, Object>();

    SplitDocument doc = candidateList.getDoc();

    FeatureDomain[] featureDomains =
      candidateList.getFeatureDomains();
    Params params = candidateList.getParams();
    Statistics statistics = candidateList.getStatistics();
    String base = candidateList.getBase();

    map.put("path", doc.path);
    map.put("loc", loc);

    Object[] candidates = new Object[numMultiCandidates];
    for (int i=0; i<numMultiCandidates; i++) {
      candidates[i] =
        toMap(candidateList.get(i), whitespaceModel,
              featureDomains, params, statistics, doc, base);
    }
    map.put("candidates", candidates);

    return map;
  }

  static Map<String, Object>
  toMap(MultiTokenCandidate candidate,
        WhitespaceModel whitespaceModel,
        FeatureDomain[] featureDomains, Params params,
        Statistics statistics, SplitDocument doc, String base) {
    Map<String, Object> map = new HashMap<String, Object>();

    map.put("candidate", whitespaceModel.toString(candidate));
    MultiTokenCandidate[] candidates =
      new MultiTokenCandidate[candidate.getLength()];
    int idx = candidate.getLength()-1;
    for (MultiTokenCandidate curr = candidate; curr != null;
         curr = curr.getPrev()) {
      candidates[idx] = curr;
      idx--;
    }

    Object[] candidateInfos = new Object[candidates.length];
    for (int i=0; i<candidates.length; i++) {
      candidateInfos[i] =
        candidatesToMap(candidates[i], whitespaceModel,
                        featureDomains, params, statistics, doc,
                        base);
    }
    map.put("pieces", candidateInfos);
    map.put("nextCandidates", 
            nextCandidatesToMap(candidate, whitespaceModel,
                                featureDomains, params, statistics,
                                doc));

    return map;
  }

  static Map<String, Object>
  candidatesToMap(MultiTokenCandidate candidate,
                  WhitespaceModel whitespaceModel,
                  FeatureDomain[] featureDomains, Params params,
                  Statistics statistics, SplitDocument doc,
                  String base) {
    Map<String, Object> map = new HashMap<String, Object>();

    MultiTokenCandidate prev = candidate.getPrev();
    double prevProb = prev == null ? 1.0 : prev.getProb();
    int maxCandidates = (int) (Main.maxCandidates * prevProb);
    PredictionContext context =
      new MultiTokenPredictionContext(doc, prev,
                                      prev == null ? base : "");
    CandidateList nextCandidates =
        new CandidateList(featureDomains, params, statistics,
                          context, maxCandidates);

    map.put("prob", candidate.getProb());
    map.put("exclusiveProb", candidate.getExclusiveProb());
    map.put("context", NgramContext.get(context).contextStr());
    map.put("maxCandidates", maxCandidates);
    map.put("numCandidates", nextCandidates.size());
    Candidate last = candidate.getLast();
    map.put("this", InferStateToMap.toMap(last, featureDomains,
                                          params, context));
    List<Object> candidateInfos = new ArrayList<Object>();
    int len = Math.min(numCandidates, nextCandidates.size());
    for (int i=0; i<len; i++) {
      Candidate c = nextCandidates.get(i);
      if (c.token.equals(last.token)) continue;
      candidateInfos.add(InferStateToMap.toMap(c, featureDomains,
                                               params, context));
    }
    map.put("alternatives", candidateInfos);

    return map;
  }

  static Map<String, Object>
  nextCandidatesToMap(MultiTokenCandidate prev,
                      WhitespaceModel whitespaceModel,
                      FeatureDomain[] featureDomains,
                      Params params, Statistics statistics,
                      SplitDocument doc) {
    Map<String, Object> map = new HashMap<String, Object>();

    double prevProb = prev.getProb();
    int maxCandidates = (int) (Main.maxCandidates * prevProb);
    PredictionContext context =
      new MultiTokenPredictionContext(doc, prev, "");
    CandidateList nextCandidates =
        new CandidateList(featureDomains, params, statistics,
                          context, maxCandidates);

    map.put("context", NgramContext.get(context).contextStr());
    map.put("maxCandidates", maxCandidates);
    map.put("numCandidates", nextCandidates.size());
    List<Object> candidateInfos = new ArrayList<Object>();
    int len = Math.min(numCandidates, nextCandidates.size());
    for (int i=0; i<len; i++) {
      Candidate c = nextCandidates.get(i);
      candidateInfos.add(InferStateToMap.toMap(c, featureDomains,
                                               params, context));
    }
    map.put("candidates", candidateInfos);

    return map;
  }
}
