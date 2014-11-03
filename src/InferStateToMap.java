package smartAutocomplete;

import java.util.*;

import fig.basic.*;

public class InferStateToMap {
  public static Map<String, Object> toMap(InferState state, int numCandidates) {
    Map<String, Object> map = new HashMap<String, Object>();

    FeatureDomain[] featureDomains = state.getFeatureDomains();
    Params params = state.getParams();
    PredictionContext context = state.getContext();

    map.put("path", context.getPath());
    map.put("loc", state.getTrueToken().loc());
    map.put("context", NgramContext.get(context).contextStr());

    Object[] candidates = new Object[numCandidates];
    for (int i=0; i<numCandidates; i++) {
      candidates[i] =
        toMap(state.getCandidates().get(i), featureDomains, params, context);
    }
    map.put("candidates", candidates);

    map.put("oracle", state.isOracle());
    map.put("oov", state.isOov());
    map.put("ident", state.isIdent());
    map.put("isActualToken",
            state.getTrueToken().type() == Token.Type.ACTUAL);
    map.put("entropy", state.isOracle() ? state.getEntropy() : "NaN");
    map.put("rank", state.getRank()+1);
    map.put("trueToken",
            toMap(state.getTrueCandidate(), featureDomains, params, context));

    Map<String, Object> diff = new HashMap<String, Object>();
    Map<String, Double> featureDiff =
      FeatureVector.getFeatureDiff(state.getTrueCandidate().features,
                                   state.getCandidates().get(0).features);
    putFeatureWeights(diff, featureDiff, params);
    map.put("diff", diff);

    return map;
  }

  static Map<String, Object>
  toMap(Candidate candidate, FeatureDomain[] featureDomains, Params params,
        PredictionContext context) {
    Map<String, Object> map = new HashMap<String, Object>();

    map.put("token", candidate.token);
    map.put("prob", Double.isNaN(candidate.prob) ? "NaN" : candidate.prob);

    Map<String, Object> featureInfos = new HashMap<String, Object>();
    for (FeatureDomain featureDomain : featureDomains) {
      if (featureDomain.isActive(context.getPath())) {
        featureDomain.putInfo(featureInfos, context, candidate);
      }
    }
    map.put("featureInfos", featureInfos);

    putFeatureWeights(map, candidate.features.toMap(), params);
    return map;
  }

  public static void putFeatureWeights(Map<String, Object> map,
                                       Map<String, Double> features,
                                       Params params) {
    List<Map.Entry<String, Double>> entries =
      FeatureVector.getFeatureWeights(features, params);
    double sumValue = 0;
    List<Object> featureWeights = new ArrayList<Object>();
    for (Map.Entry<String, Double> entry : entries) {
      double value = entry.getValue();
      sumValue += value;
      String feature = entry.getKey();

      Map<String, Object> featureWeight = new HashMap<String, Object>();
      featureWeight.put("value", value);
      featureWeight.put("weight", params.getWeight(feature));
      featureWeight.put("feature", feature);
      featureWeight.put("featureVal",
                        MapUtils.getDouble(features, feature, 0));
      featureWeights.add(featureWeight);
    }
    map.put("sum", sumValue);
    map.put("featureWeights", featureWeights);
  }
}
