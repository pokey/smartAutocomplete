package smartAutocomplete;

import com.google.common.collect.Lists;

import java.util.*;

import fig.basic.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

public class Gradient {
  private static void logGradient(Candidate trueCandidate,
                                  List<Candidate> candidates,
                                  HashMap<String, Double> gradient,
                                  double[] idealDist) {
    begin_track("tune");
    begin_track("true (%s)", trueCandidate.token);
    for (int i = 0; i < Main.clusters; i++) { // Want this
      trueCandidate.clusterFeatures.logFeatureVals("cluster " + i,
          idealDist[i], Main.clusterDecorators[i]);
    }
    end_track();

    for (int i = 0; i < candidates.size(); i++) {  // Predicted this
      Candidate candidate = candidates.get(i);
      begin_track(candidate.token);
      for (int j = 0; j < Main.clusters; j++) {
        candidate.clusterFeatures.logFeatureVals("cluster " + j,
            -candidate.clusterProbs[j], Main.clusterDecorators[j]);
      }
      end_track();
    }
    List<Map.Entry<String, Double>> entries = Lists.newArrayList(gradient.entrySet());
    for (Map.Entry<String, Double> entry : entries) {
      double value = entry.getValue();
      logs("%s\t%s", entry.getKey(), value);
    }
    end_track();
  }

  // Compute negative of gradient (true - predicted)
  public static HashMap<String, Double>
  computeGradient(Candidate trueCandidate, List<Candidate> candidates) {
    HashMap<String, Double> gradient = new HashMap<String, Double>(); 

    // Compute conditional probabilities for clusters conditioned on
    // trueCandidate
    double[] idealDist = new double[Main.clusters];
    System.arraycopy(trueCandidate.clusterScores, 0, idealDist, 0, Main.clusters);
    NumUtils.expNormalize(idealDist);

    for (int i = 0; i < Main.clusters; i++) // Want this
      trueCandidate.clusterFeatures.increment(idealDist[i], gradient,
                                              Main.clusterDecorators[i]);
    trueCandidate.features.increment(1, gradient);

    for (int i = 0; i < candidates.size(); i++) {  // Predicted this
      Candidate candidate = candidates.get(i);
      for (int j = 0; j < Main.clusters; j++)
        candidate.clusterFeatures.increment(-candidate.clusterProbs[j],
                                            gradient, Main.clusterDecorators[j]);
      candidate.features.increment(-candidate.prob, gradient);
    }

    // if (Main.verbose >= 2) logGradient(trueCandidate, candidates, gradient, idealDist);

    return gradient;
  }
}

