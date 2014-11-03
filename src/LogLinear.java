package smartAutocomplete;

import java.util.*;

class LogLinear {
  // Computes probabilities for candidates
  static void computeProbabilities(List<Candidate> candidates, int clusters) {
    // Compute max score to prevent overflow
    double max = Double.NEGATIVE_INFINITY;
    for (Candidate candidate : candidates)
      for (int j = 0; j < clusters; j++)
        max = Math.max(max, candidate.clusterScores[j]);

    // Exponentiate and compute sum
    double sum = 0;
    for (Candidate candidate : candidates)
      for (int j = 0; j < clusters; j++) {
        double clusterProb = Math.exp(candidate.clusterScores[j]-max);
        sum += clusterProb;
        candidate.clusterProbs[j] = clusterProb;
      }

    // Normalize and compute candidate probabilities
    for (Candidate candidate : candidates) {
      double prob = 0;
      for (int j = 0; j < clusters; j++) {
        double clusterProb = candidate.clusterProbs[j] / sum;
        prob += clusterProb;
        candidate.clusterProbs[j] = clusterProb;
      }
      candidate.prob = prob;
    }
  }
}
