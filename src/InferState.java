package smartAutocomplete;

import fig.basic.*;

// State associated with doing inference in one example.
// Task: predict doc.tokens[i] given everything in the past.
public class InferState {
  // Constructor arguments
  private final FeatureDomain[] featureDomains;
  private final Params params;
  private final Statistics statistics;
  private final PredictionContext context;
  private final Token trueToken;

  // Accessors for constructor arguments
  public FeatureDomain[] getFeatureDomains() { return featureDomains; }
  public Params getParams() { return params; }
  public Statistics getStatistics() { return statistics; }
  public PredictionContext getContext() { return context; }
  public Token getTrueToken() { return trueToken; }

  // Computed values
  private Candidate trueCandidate = null;
  private CandidateList candidates;
  private boolean oracle = false; // whether any candidate is correct
  private boolean oov = false;
  private boolean ident = false;
  private boolean correct = false; // whether highest scoring candidate is
                                   // correct.
  private double entropy = Double.NaN;
  private int rank = 0;

  // Accessors for computed values
  public Candidate getTrueCandidate() { return trueCandidate; }
  public CandidateList getCandidates() { return candidates; }
  public boolean isOracle() { return oracle; }
  public boolean isOov() { return oov; }
  public boolean isIdent() { return ident; }
  public boolean isCorrect() { return correct; }
  public double getEntropy() { return entropy; }
  public int getRank() { return rank; }

  public double getReciprocalRank() {
    return oracle ? 1.0/(double)(rank+1) : 0;
  }

  public InferState(FeatureDomain[] featureDomains, Params params,
                    Statistics statistics, PredictionContext context,
                    Token trueToken) {
    this.featureDomains = featureDomains;
    this.params = params;
    this.statistics = statistics;
    this.context = context;
    this.trueToken = trueToken;
    this.candidates =
      new CandidateList(featureDomains, params, statistics, context);

    // True answer.
    String trueTokenStr = trueToken.str();

    // Is the true token an identifier?
    ident = (trueToken.type() == Token.Type.ACTUAL &&
             trueToken.context() == Token.Context.NONE &&
             Tokenizer.isIdentifier(trueTokenStr));

    // Find true token on the beam and set oracle, trueCandidate and rank
    for (Candidate candidate : candidates) {
      if (candidate.token.equals(trueTokenStr)) {
        oracle = true;
        trueCandidate = candidate;
        break;
      }
      rank++;
    }

    // Correct?
    if (candidates.size() > 0) {
      correct = candidates.get(0).token.equals(trueTokenStr);
    }

    if (!oracle) {
      // FixMe: [correctness] When we have global corpus, use vocab from that
      Vocab vocab;
      if (Main.isFixedVocab()) {
        vocab = Main.getVocab();
      } else {
        Corpus corpus = statistics.getProjectLangCorpus(context.getPath());
        DataSummary inDomainSummary =
          statistics.getStatistic(NgramKNCounts.class, corpus).getSummary();
        vocab = inDomainSummary.getVocab();
      }
      oov = !vocab.contains(trueTokenStr);

      trueCandidate = new Candidate(trueTokenStr, Main.clusters);
      trueCandidate.computeScores(featureDomains, params, context,
                                  Main.clusters);
    }

    // Compute entropy
    entropy = oracle ? -Math.log(trueCandidate.prob) : Double.NaN;
  }
};
