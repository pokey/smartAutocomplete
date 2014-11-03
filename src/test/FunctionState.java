package smartAutocomplete;

import com.google.common.collect.Lists;

import java.util.*;

// For use in gradcheck
class FunctionState implements GradCheck.FunctionState {
  private InferState state = null;

  // These are for the InferState object
  private final FeatureDomain[] featureDomains;
  private final Params params;
  private final Statistics statistics;
  private final PredictionContext context;
  private final Token trueToken;

  public FunctionState(FeatureDomain[] featureDomains, Params params,
                       Statistics statistics, PredictionContext context,
                       Token trueToken) {
    this.featureDomains = featureDomains;
    this.params = params;
    this.statistics = statistics;
    this.context = context;
    this.trueToken = trueToken;
  }

  public HashMap<String, Double> point() {
    return params.weights;
  }

  public double value() {
    if (state == null) {
      state = new InferState(featureDomains, params, statistics, context, trueToken);
    } 
    return state.getEntropy();
  }

  public HashMap<String, Double> gradient() {
    if (state == null) {
      state = new InferState(featureDomains, params, statistics, context, trueToken);
    }

    HashMap<String, Double> grad =
      Gradient.computeGradient(state.getTrueCandidate(),
                               state.getCandidates().list());
    List<Map.Entry<String, Double>> entries =
      Lists.newArrayList(grad.entrySet());
    for (Map.Entry<String, Double> entry : entries)
      grad.put(entry.getKey(), -entry.getValue());

    return grad;
  }

  public void invalidate() {
    state = null;
  }
};

