package smartAutocomplete;

import java.util.*;

import fig.basic.*;

// A candidate token to predict.
public class Candidate {
  public final String token;
  final FeatureVector features = new FeatureVector();
  final FeatureVector clusterFeatures = new FeatureVector();
  final double[] clusterScores;
  final double[] clusterProbs;
  double prob = Double.NaN;

  public Candidate(String token) {
    this(token, Main.clusters);
  }

  public Candidate(String token, int clusters) {
    this.token = token;
    clusterScores = new double[clusters];
    clusterProbs = new double[clusters];
    Arrays.fill(clusterScores, Double.NaN);
    Arrays.fill(clusterProbs, Double.NaN);
  }

  public void computeScores(FeatureDomain[] featureDomains, Params params,
                            PredictionContext context, int clusters) {
    // Add features
    for (FeatureDomain featureDomain : featureDomains) {
      if (featureDomain.isActive(context.getPath())) {
        featureDomain.addFeatures(context, this);
      }
    }

    // Compute score
    double score = features.dotProduct(params);
    for (int i=0; i<clusters; i++)
      clusterScores[i] = score +
        clusterFeatures.dotProduct(params, Main.clusterDecorators[i]);
  }
}
