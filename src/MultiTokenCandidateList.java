package smartAutocomplete;

import java.util.*;

import fig.basic.*;

import smartAutocomplete.util.*;
import static fig.basic.LogInfo.begin_track;
import static fig.basic.LogInfo.logs;
import static fig.basic.LogInfo.end_track;

public class MultiTokenCandidateList implements Iterable<MultiTokenCandidate> {
  @Option(gloss="Threshold probability for considering candidates")
    public static double threshold = .1;
  @Option(gloss="Maximum number of candidates to expand at each level")
    public static int branchingFactor = 5;
  @Option(gloss="Minimum number of candidates to display")
    public static int minSize = 50;

  private FeatureDomain[] featureDomains;
  private Params params;
  private Statistics statistics;
  private SplitDocument doc;
  private String base;

  public SplitDocument getDoc() { return doc; }
  public FeatureDomain[] getFeatureDomains() {
    return featureDomains;
  }
  public Params getParams() { return params; }
  public Statistics getStatistics() { return statistics; }
  public String getBase() { return base; }

  private List<MultiTokenCandidate> candidates =
    new ArrayList<MultiTokenCandidate>();

  public int size() { return candidates.size(); }
  public List<MultiTokenCandidate> list() { return candidates; }
  public MultiTokenCandidate get(int idx) {
    return candidates.get(idx);
  }

  public Iterator<MultiTokenCandidate> iterator() {
    return candidates.iterator();
  }

  private void computeCandidateList(MultiTokenCandidate prev) {
    double prevProb = prev == null ? 1.0 : prev.getProb();
    int maxCandidates = (int) (Main.maxCandidates * prevProb);

    PredictionContext context =
      new MultiTokenPredictionContext(doc, prev, prev == null ? base : "");
    CandidateList nextCandidates =
        new CandidateList(featureDomains, params, statistics, context, maxCandidates);
    double currThreshold = threshold / prevProb;
    // FixMe: [correctness] Hack to stop expanding if we are trying to predict
    // what is already there
    String suffixFirst = context.getSuffix().size() > 0 ?
                         context.getSuffix().get(0).str() : "";

    int count = 1;
    for (Candidate candidate : nextCandidates) {
      if (candidate.prob < currThreshold ||
          count > branchingFactor) {
        if (prev == null && candidates.size() < minSize) {
          candidates.add(new MultiTokenCandidate(prev, candidate));
          continue;
        } else break;
      }
      if (prev != null && candidate.token.equals(suffixFirst)) {
        continue;
      }

      count++;
      MultiTokenCandidate multiCandidate =
        new MultiTokenCandidate(prev, candidate);
      multiCandidate.subtractProbFromPrev();
      candidates.add(multiCandidate);
      computeCandidateList(multiCandidate);
    }
  }

  public MultiTokenCandidateList(FeatureDomain[] featureDomains,
                                 Params params, Statistics statistics,
                                 SplitDocument doc, String base) {
    this.featureDomains = featureDomains;
    this.params = params;
    this.statistics = statistics;
    this.doc = doc;
    this.base = base;

    computeCandidateList(null);

    // Sort candidates by decreasing probability
    Collections.sort(candidates, new Comparator<MultiTokenCandidate>() {
      public int compare(MultiTokenCandidate c1, MultiTokenCandidate c2) {
        if (c1.getExclusiveProb() > c2.getExclusiveProb()) return -1;
        if (c1.getExclusiveProb() < c2.getExclusiveProb()) return +1;
        return 0;
      }
    });
  }
}
